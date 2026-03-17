package com.ta.managementproject.restcontroller;


import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {
    @Autowired
    private UserService userService;

    @PostMapping("")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request, BindingResult bindingResult){
        return userService.addNewUser(request);
    }

}
