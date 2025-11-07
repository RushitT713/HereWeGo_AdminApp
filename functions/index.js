/**
 * Import necessary Firebase modules.
 * functions: For defining Cloud Functions triggers.
 * logger: For logging information (useful for debugging).
 * admin: For interacting with Firebase services like Firestore and FCM.
 */
const functions = require("firebase-functions");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK.
admin.initializeApp();

/**
 * Sanitizes a club name into a valid FCM topic name.
 * Example: "Real Madrid" -> "club_real_madrid_all"
 * Removes special chars, converts to lowercase, adds prefix/suffix.
 * @param {string} name The club name.
 * @param {string} suffix The suffix ("all" or "official").
 * @return {string} The sanitized topic name, or empty string if invalid.
 */
function sanitizeForTopic(name, suffix) {
  if (!name || typeof name !== "string") {
    return ""; // Return empty if name is invalid
  }
  // Replace non-alphanumeric, keep spaces, trim, lowercase, replace spaces
  const sanitized = name.toLowerCase()
      .replace(/[^a-z0-9\s]/g, "") // Remove most special chars
      .trim() // Trim whitespace
      .replace(/\s+/g, "_"); // Replace spaces with underscores
  // Ensure no multiple underscores or leading/trailing underscores
  const finalSanitized = sanitized.replace(/_+/g, "_").replace(/^_+|_+$/g, "");

  if (!finalSanitized) return ""; // Return empty if sanitization fails

  return `club_${finalSanitized}_${suffix}`;
}


/**
 * Cloud Function triggered whenever a document in 'news_items' is written.
 */
exports.sendNewsNotification = functions.firestore
    .document("news_items/{newsId}")
    .onWrite(async (change, context) => {
      // Get data after the change, null if deleted.
      const newsItem = change.after.exists ? change.after.data() : null;
      // Get data before the change, null if new.
      // FIX: Broke line 49 to satisfy max-len
      const previousNewsItem = change.before.exists ?
          change.before.data() : null;
      const newsId = context.params.newsId;

      // --- Exit early if the document was deleted ---
      if (!newsItem) {
        logger.log(`News item ${newsId} deleted. No notification.`);
        return null;
      }

      // --- Log basic information ---
      logger.log(
          `Processing write for news item: ${newsId}`,
          {before: previousNewsItem, after: newsItem},
      );

      // --- Determine event type ---
      const isNewItem = !change.before.exists;
      const isUpdate = change.before.exists && change.after.exists;

      // --- Extract relevant data fields (with defaults) ---
      const playerName = newsItem.playerName || "Player";
      const fromTo = newsItem.fromTo || "Transfer details";
      const milestoneStatus = newsItem.milestoneStatus || 1;
      const isBreakingNews = newsItem.isBreakingNews || false;

      // --- Build the basic notification payload ---
      const notificationPayload = {
        notification: {
          title: `Transfer Update: ${playerName}`,
          body: `${fromTo}`,
          // Optional: Add icon/sound
          // icon: 'ic_notification',
          // sound: 'default',
        },
        android: {
          notification: {
            // Matches channel ID in MyFirebaseMessagingService
            channel_id: "here_we_go_news",
            // Optional: Customize color, visibility etc.
            // color: "#FFC107",
          },
        },
        // Optional: Add 'data' payload for custom background handling
        // data: { newsId: newsId, /* other data */ },
      };

      // --- List of topics to send the notification to ---
      const topicsToSendTo = new Set();

      // --- Topic Determination Logic ---

      // 1. Item-Specific Topic: Notify on any change.
      if (newsId) {
        topicsToSendTo.add(`item_${newsId}`);
      }

      // 2. Club-Specific Topics: Notify on NEW items or milestone CHANGES.
      const milestoneChanged = isUpdate &&
          newsItem.milestoneStatus !== previousNewsItem.milestoneStatus;
      if (isNewItem || milestoneChanged) {
        const clubs = (fromTo.split("→") || [])
            .map((c) => c.trim()).filter(Boolean);
        clubs.forEach((club) => {
          // Add the general club topic (e.g., club_real_madrid_all)
          const allTopic = sanitizeForTopic(club, "all");
          if (allTopic) topicsToSendTo.add(allTopic);

          // If current milestone is "Official" (status 5), add official topic.
          if (Math.abs(milestoneStatus) === 5) { // 5 = Official
            const officialTopic = sanitizeForTopic(club, "official");
            if (officialTopic) topicsToSendTo.add(officialTopic);
          }
        });
      }

      // 3. Breaking News Topic: Notify on NEW breaking items or when an item
      //    is UPDATED to become breaking news.
      const becameBreaking = isUpdate && isBreakingNews &&
          !(previousNewsItem.isBreakingNews || false);
      if ((isNewItem && isBreakingNews) || becameBreaking) {
        topicsToSendTo.add("breaking_news");
      }

      // --- Filtering and Sending ---

      // Remove any potentially empty/invalid topics
      const validTopics = Array.from(topicsToSendTo).filter(Boolean);

      if (validTopics.length === 0) {
        logger.log(`No relevant topics or conditions met for ${newsId}. ` +
                   "No notification sent.");
        return null; // Stop execution
      }

      logger.log(`Attempting send for ${newsId} to topics:`, validTopics);

      // Send one FCM message per topic.
      const sendPromises = validTopics.map((topic) => {
        return admin.messaging().send({
          ...notificationPayload, // Common payload
          topic: topic, // Target specific topic
        }).then((response) => {
          logger.log(`Sent to topic ${topic} for ${newsId}:`, response);
          return response;
        }).catch((error) => {
          logger.error(`Failed send to topic ${topic} for ${newsId}:`, error);
          // Check for specific errors
          if (error.code === "messaging/invalid-argument" ||
              error.message.includes("invalid-topic-name")) {
            logger.warn(`Invalid topic name: ${topic}. Check sanitization.`);
          }
          return null; // Resolve even on error for Promise.all
        });
      });

      // Wait for all individual send operations to settle.
      try {
        await Promise.all(sendPromises);
        logger.log(`Finished send attempts for ${newsId} ` +
                   `to ${validTopics.length} topics.`);
      } catch (error) {
        logger.error("Unexpected error during batch send for " +
                     `${newsId}:`, error);
      }

      return null; // Indicate successful completion
    });

