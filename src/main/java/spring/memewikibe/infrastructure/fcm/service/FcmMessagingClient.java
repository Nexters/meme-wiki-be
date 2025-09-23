package spring.memewikibe.infrastructure.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;

public interface FcmMessagingClient {
    BatchResponse sendEachForMulticast(MulticastMessage message) throws FirebaseMessagingException;
}
