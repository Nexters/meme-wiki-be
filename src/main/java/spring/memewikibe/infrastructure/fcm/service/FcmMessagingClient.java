package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;

/**
 * Client interface for sending Firebase Cloud Messaging (FCM) notifications.
 * <p>
 * This interface abstracts the Firebase Messaging API to allow for easier testing
 * and potential alternative implementations.
 */
public interface FcmMessagingClient {
    /**
     * Sends a multicast message to multiple device tokens.
     *
     * @param message the multicast message to send, containing notification content and target tokens
     * @return a {@link BatchResponse} containing the results of sending to each token
     * @throws FirebaseMessagingException if an error occurs while sending the message
     */
    BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException;
}
