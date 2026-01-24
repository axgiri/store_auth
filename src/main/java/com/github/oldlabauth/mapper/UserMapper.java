package com.github.oldlabauth.mapper;

import com.github.oldlabauth.dto.response.UserResponse;
import com.github.oldlabauth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {


    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getEmail()
        );
    }
}


