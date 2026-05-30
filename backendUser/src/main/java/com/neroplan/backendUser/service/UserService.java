package com.neroplan.backendUser.service;

import com.neroplan.backendUser.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;


    public void googleLogin(){
        return ;
    }


}
