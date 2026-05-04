package com.ta.managementproject.service.user;

import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.entity.ProjectManager;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.repository.ProjectManagerDb;
import com.ta.managementproject.repository.ProjectMemberDb;
import com.ta.managementproject.repository.RoleDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class UserServiceImpl implements UserService{
    private final UserDb userDb;
    private final RoleDb roleDb;
    private final AESUtil aesUtil;
    private final ProjectManagerDb projectManagerDb;
    private final ProjectMemberDb projectMemberDb;
    private final HttpServletRequest request;
    private final JwtUtils jwtUtils;
    private final UtilService utilService;
    private final AuthService authService;

    public UserServiceImpl(
            UserDb userDb,
            RoleDb roleDb,
            AESUtil aesUtil,
            ProjectManagerDb projectManagerDb,
            ProjectMemberDb projectMemberDb,
            HttpServletRequest request,
            JwtUtils jwtUtils,
            UtilService utilService,
            AuthService authService
    ) {
        this.userDb = userDb;
        this.roleDb = roleDb;
        this.aesUtil = aesUtil;
        this.projectManagerDb = projectManagerDb;
        this.projectMemberDb = projectMemberDb;
        this.request = request;
        this.jwtUtils = jwtUtils;
        this.utilService = utilService;
        this.authService = authService;
    }

    @Override // Total CYC: 8, LOC: 57, COG: 7
    public ResponseEntity<?> addNewUser(RegisterRequestDTO requestDTO) throws Exception { // CYC: 6, LOC: 41, COG: 7
        String username = requestDTO.getUsername();
        String fullName = requestDTO.getFullName();
        String password = requestDTO.getPassword();

        if (userDb.findByUsername(username) != null){
            throw new ConflictException("Username already exist!");
        }

        if (requestDTO.getRole() != 2 && requestDTO.getRole() != 1){
            throw new BadRequestException("Selected role is not valid!");
        }

        //Role: 1 (ProjectManager), 2 (ProjectMember)
        User newUser;

        if (requestDTO.getRole() == 1){
            ProjectManager projectManager = ProjectManager.builder()
                    .role(roleDb.findByName("PROJECT_MANAGER"))
                    .username(username)
                    .password(aesUtil.encrypt(password)) // CYC: 1, LOC: 7, COG: 0
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

        return utilService.buildResponse( // CYC: 1, LOC: 9, COG: 0
                HttpStatus.CREATED,
                String.format("Username %s berhasil didaftarkan", requestDTO.getUsername()),
                new CrudResponseDTO("SUCCESS", String.format("Username %s berhasil didaftarkan", requestDTO.getUsername())));
    }

    @Override // CYC: 1, LOC: 5, COG: 0
    public Role getUserRoleByUsername(String username) {
        String role = userDb.getRoleByUsername(username);
        return Role.valueOf(role);
    }
}
