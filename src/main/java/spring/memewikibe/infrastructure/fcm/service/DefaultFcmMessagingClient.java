package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link FcmMessagingClient} using Firebase Admin SDK.
 * <p>
 * This component is conditionally enabled when the FCM service account key path
 * is configured in application properties.
 *
 * @see FcmMessagingClient
 */
@Component
@ConditionalOnProperty(prefix = "fcm", name = "service-account-key-path", matchIfMissing = false)
public class DefaultFcmMessagingClient implements FcmMessagingClient {
    /**
     * {@inheritDoc}
     */
    @Override
    public BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException {
        return FirebaseMessaging.getInstance().sendEachForMulticast(message);
    }
}
