package pl.schoolmanager.service.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.schoolmanager.bean.SessionManager;
import pl.schoolmanager.entity.Message;
import pl.schoolmanager.entity.MessageData;
import pl.schoolmanager.entity.User;
import pl.schoolmanager.repository.MessageDataRepository;
import pl.schoolmanager.repository.MessageRepository;
import pl.schoolmanager.repository.UserRepository;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageDataRepository messageDataRepository;

    @Autowired
    private SessionManager sessionManager;

    @Override
    public Message save(Message message) {
        String receiverEmail = message.getReceiverEmail();
        if (isEmailValid(receiverEmail)) {
            message.setSender(sessionManager.loggedUser());
            message.setReceiver(userRepository.findOneByEmail(receiverEmail));
            message.setMessageData(createMessageData(message));
            message = messageRepository.save(message);
            sessionManager.updateMessageValues();
            return message;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Message removeReceiver(long messageId) {
        Message message = messageRepository.findOne(messageId);
        message.setReceiver(null);
        return saveOrDelete(message);
    }

    @Override
    public Message removeSender(long messageId) {
        Message message = messageRepository.findOne(messageId);
        message.setSender(null);
        return saveOrDelete(message);
    }

    @Override
    public Message markAsRead(long messageId) {
        Message message = messageRepository.findOne(messageId);
        message.setChecked(1);
        message = messageRepository.save(message);
        sessionManager.updateMessageValues();
        return message;
    }

    @Override
    public List<Message> receivedMessages(User user) {
        return messageRepository.findAllByReceiver(user);
    }

    @Override
    public List<Message> sentMessages(User user) {
        return messageRepository.findAllBySender(user);
    }

    @Override
    public Message message(long id) {
        return messageRepository.findOne(id);
    }

    private Message saveOrDelete(Message message) {
        if (isMessageReadyToRemove(message)) {
            messageRepository.delete(message.getId());
        } else {
            messageRepository.save(message);
        }
        sessionManager.updateMessageValues();
        return message;
    }

    private boolean isMessageReadyToRemove(Message message) {
        return message.getReceiver() == null && message.getSender() == null;
    }

    private MessageData createMessageData(Message message) {
        MessageData messageData = new MessageData(message);
        return messageDataRepository.save(messageData);
    }

    private boolean isEmailValid(String email) {
        return isEmailExists(email) && !isLoggedUserEmail(email);
    }

    private boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    private boolean isLoggedUserEmail(String email) {
        return sessionManager.loggedUser().getEmail().equals(email);
    }

}
