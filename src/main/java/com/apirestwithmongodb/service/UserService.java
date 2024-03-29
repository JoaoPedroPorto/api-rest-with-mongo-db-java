package com.apirestwithmongodb.service;

import com.apirestwithmongodb.constant.MessageEnum;
import com.apirestwithmongodb.constant.StatusEnum;
import com.apirestwithmongodb.dao.UserDAO;
import com.apirestwithmongodb.exception.ApplicationException;
import com.apirestwithmongodb.exception.PreConditionFailedException;
import com.apirestwithmongodb.model.Users;
import com.apirestwithmongodb.util.ApplicationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    UserDAO userDAO;

    public List<Users> listAllUsers() {
        log.info("Listagem de usuários");
        return userDAO
                .findAllByStatusNotOrderByNameAsc(StatusEnum.INACTIVE.getStatus())
                .collect(Collectors.toList());
    }

    public void createUser(Users user) throws ApplicationException, PreConditionFailedException {
        log.info("Criação de usuário");
        if ((user.getName() == null || user.getName().trim().isEmpty()) ||
                (user.getMail() == null || user.getMail().trim().isEmpty()))
            throw new PreConditionFailedException();
        if (!ApplicationUtil.isMailValid(user.getMail().trim()))
            throw new ApplicationException(MessageEnum.MAIL_INVALID.getMessage());
        Optional<Users> userDB = userDAO.findOneByMail(user.getMail().trim());
        if (userDB != null && userDB.isPresent() && !userDB.get().getStatus().equals(StatusEnum.INACTIVE))
            throw new ApplicationException(MessageEnum.EMAIL_EXIST_ERROR.getMessage());
        // EDITA USUARIO
        if (userDB != null && userDB.isPresent()) {
            userDB.get().setName(user.getName().trim());
            userDB.get().setPassword(ApplicationUtil.generatePassword());
            userDB.get().setStatus(StatusEnum.PENDING.getStatus());
            userDB.get().setEditedDate(new Date());
            Users usersaved = userDAO.save(userDB.get());
            if (usersaved == null) throw new ApplicationException(MessageEnum.CREATE_USER_ERROR.getMessage());
            return;
        }
        // CRIA USUARIO
        user.setStatus(StatusEnum.PENDING.getStatus());
        user.setPassword(ApplicationUtil.generatePassword());
        user.setName(user.getName().trim());
        user.setMail(user.getMail().trim());
        user.setCreatedDate(new Date());
        Users usersaved = userDAO.save(user);
        if (usersaved == null) throw new ApplicationException(MessageEnum.CREATE_USER_ERROR.getMessage());
    }

    public void updateUser(String id, Users user) throws ApplicationException, PreConditionFailedException {
        log.info("Atualização de usuário");
        if ((id == null || id.trim().isEmpty()) ||
                (user.getName() == null || user.getName().trim().isEmpty()))
            throw new PreConditionFailedException();
        Optional<Users> userDB = userDAO
                .findOneByIdAndStatusNot(id, StatusEnum.INACTIVE.getStatus());
        if (userDB == null || !userDB.isPresent())
            throw new ApplicationException(MessageEnum.USER_NOT_FOUND.getMessage());
        userDB.get().setName(user.getName().trim());
        userDB.get().setEditedDate(new Date());
        Users userSaved = userDAO.save(userDB.get());
        if (userSaved == null) throw new ApplicationException(MessageEnum.UPDATE_USER_ERROR.getMessage());
    }

    public void deleteUser(String id) throws ApplicationException, PreConditionFailedException {
        log.info("Exclusão de um usuário");
        if (id == null || id.trim().isEmpty())
            throw new PreConditionFailedException();
        Optional<Users> userDB = userDAO
                .findOneByIdAndStatusNot(id, StatusEnum.INACTIVE.getStatus());
        if (userDB == null || !userDB.isPresent())
            throw new ApplicationException(MessageEnum.USER_NOT_FOUND.getMessage());
        userDB.get().setStatus(StatusEnum.INACTIVE.getStatus());
        userDB.get().setEditedDate(new Date());
        Users userSaved = userDAO.save(userDB.get());
        if (userSaved == null) throw new ApplicationException(MessageEnum.DELETE_USER_ERROR.getMessage());
    }

}
