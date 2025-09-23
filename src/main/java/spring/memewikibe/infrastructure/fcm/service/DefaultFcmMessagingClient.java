package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import org.springframework.stereotype.Component;

@Component
public class DefaultFcmMessagingClient implements FcmMessagingClient {
    @Override
    public BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException {
        return FirebaseMessaging.getInstance().sendEachForMulticast(message);
    }
}
