package com.ta.managementproject.service.user;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.entity.ProjectManager;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.repository.ProjectManagerDb;
import com.ta.managementproject.repository.ProjectMemberDb;
import com.ta.managementproject.repository.RoleDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@Transactional
public class UserServiceImpl implements UserService{

    @Autowired
    private UserDb userDb;

    @Autowired
    private RoleDb roleDb;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private ProjectManagerDb projectManagerDb;

    @Autowired
    private ProjectMemberDb projectMemberDb;

    @Override
    public ResponseEntity<?> addNewUser(RegisterRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        try{
            String username = requestDTO.getUsername();
            String fullName = requestDTO.getFullName();
            String password = requestDTO.getPassword();

            if (userDb.findByUsername(username) != null){
                baseResponseDTO.setStatus(HttpStatus.CONFLICT.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Username already exist!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.CONFLICT);
            }

            if (requestDTO.getRole() != 2 && requestDTO.getRole() != 1){
                baseResponseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Selected role is not valid!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.BAD_REQUEST);
            }


            //Role: 1 (ProjectManager), 2 (ProjectMember)
            User newUser;

            if (requestDTO.getRole() == 1){
                ProjectManager projectManager = ProjectManager.builder()
                        .role(roleDb.findByName("PROJECT_MANAGER"))
                        .username(username)
                        .password(aesUtil.encrypt(password))
                        .fullName(fullName)
                        .createdAt(Instant.now())
                        .build();

                newUser = projectManager;
            }else {
                ProjectMember projectMember = ProjectMember.builder()
                        .role(roleDb.findByName("PROJECT_MEMBER"))
                        .username(username)
                        .password(aesUtil.encrypt(password))
                        .fullName(fullName)
                        .createdAt(Instant.now())
                        .build();

                newUser = projectMember;
            }


            if (requestDTO.getRole() == 1){
                projectManagerDb.save((ProjectManager) newUser);
            }else{
                projectMemberDb.save((ProjectMember) newUser);
            }

            baseResponseDTO.setStatus(HttpStatus.CREATED.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format("Username %s berhasil didaftarkan", requestDTO.getUsername()));
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", String.format("Username %s berhasil didaftarkan", requestDTO.getUsername())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.CREATED);
        } catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
