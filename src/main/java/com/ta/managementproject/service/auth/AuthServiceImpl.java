package com.ta.managementproject.service.auth;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.dto.response.LoginResponseDTO;
import com.ta.managementproject.dto.response.RoleResponseDTO;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.repository.RoleDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private UserDb userDb;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleDb roleDb;

    @Override
    public ResponseEntity<?> doLogin(LoginRequestDTO request) throws Exception {
        var response = new BaseResponseDTO<LoginResponseDTO>();
        try{

            LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

            String username = request.getUsername();
            String password = request.getPassword();


            User selectedUser = userDb.findByUsername(username);

            if (selectedUser == null){
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setMessage("Username not found!");
                response.setTimestamp(new Date());
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            String encryptedPass = aesUtil.encrypt(password);

            if (encryptedPass.equals(selectedUser.getPassword())) {
                String jwtToken = jwtUtils.generateJwtToken(username, selectedUser.getRole().getName());
                loginResponseDTO.setRole(new RoleResponseDTO(selectedUser.getRole().getName()));
                loginResponseDTO.setToken(jwtToken);
                loginResponseDTO.setUsername(username);
                loginResponseDTO.setExpirationDate(jwtUtils.getExpirationFromToken(jwtToken));

                response.setStatus(HttpStatus.OK.value());
                response.setMessage("Login Successful");
                response.setTimestamp(new Date());
                response.setData(loginResponseDTO);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }else{
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setMessage("Username atau password yang dimasukkan salah!");
                response.setTimestamp(new Date());
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        }catch(Exception e){
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setTimestamp(new Date());
            response.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
