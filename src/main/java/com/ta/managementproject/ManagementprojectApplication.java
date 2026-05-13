package com.ta.managementproject;

import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootApplication
public class ManagementprojectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManagementprojectApplication.class, args);
	}

	@Bean
	@Transactional
	CommandLineRunner run(
		RoleDb roleDb, ProjectManagerDb projectManagerDb, ProjectMemberDb projectMemberDb, ProjectDb projectDb,
		StageDb stageDb, UserService userService
	){
		return args -> {
			Role role1 = Role.builder().id(1).name("PROJECT_MANAGER").build();
			Role role2 = Role.builder().id(2).name("PROJECT_MEMBER").build();
			if (roleDb.findAll().isEmpty()) {
				roleDb.saveAll(List.of(role1, role2));
			}

//			RegisterRequestDTO pm1 = RegisterRequestDTO.builder()
//					.email("manager@test.com")
//					.role(1)
//					.fullName("Project Manager 1")
//					.password("password123")
//					.build();
//
////			RegisterRequestDTO pm2 = RegisterRequestDTO.builder()
////					.email("pm2@gmail.com")
////					.role(1)
////					.fullName("Project Manager 2")
////					.password("password123")
////					.build();
//
//			RegisterRequestDTO pmb1 = RegisterRequestDTO.builder()
//					.email("member1@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb2 = RegisterRequestDTO.builder()
//					.email("member2@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb3 = RegisterRequestDTO.builder()
//					.email("member3@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb4 = RegisterRequestDTO.builder()
//					.email("member4@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb5 = RegisterRequestDTO.builder()
//					.email("member5@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb6 = RegisterRequestDTO.builder()
//					.email("member6@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb7 = RegisterRequestDTO.builder()
//					.email("member7@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb8 = RegisterRequestDTO.builder()
//					.email("member8@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb9 = RegisterRequestDTO.builder()
//					.email("member9@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//			RegisterRequestDTO pmb10 = RegisterRequestDTO.builder()
//					.email("member10@gmail.com")
//					.role(2)
//					.password("password123")
//					.build();
//
//
//			if (projectManagerDb.findAll().isEmpty()) {
//				userService.addNewUser(pm1);
////				userService.addNewUser(pm2);
//			}
//
//			if (projectMemberDb.findAll().isEmpty()) {
//				userService.addNewUser(pmb1);
//				userService.addNewUser(pmb2);
//				userService.addNewUser(pmb3);
//				userService.addNewUser(pmb4);
//				userService.addNewUser(pmb5);
//				userService.addNewUser(pmb6);
//				userService.addNewUser(pmb7);
//				userService.addNewUser(pmb8);
//				userService.addNewUser(pmb9);
//				userService.addNewUser(pmb10);
//			}
//
//			Project project1 = Project.builder()
//					.projectName("Project 1")
//					.projectManager(projectManagerDb.findByEmail("pm.1"))
//					.description("Description for project 1")
//					.build();
//
//			Project project2 = Project.builder()
//					.projectName("Project 2")
//					.projectManager(projectManagerDb.findByEmail("pm.2"))
//					.description("Description for project 2")
//					.build();
//
//			if (projectDb.findAll().isEmpty()) {
//				projectDb.saveAll(List.of(project1, project2));
//			}
//
//			Stage stage1 = Stage.builder()
//					.stageName("Stage 11")
//					.description("Description of stage 1 for project 1")
//					.project(project1)
//					.order(1)
//					.build();
//
//			Stage stage2 = Stage.builder()
//					.stageName("Stage 21")
//					.description("Description of stage 2 for project 1")
//					.project(project1)
//					.order(2)
//					.build();
//
//			Stage stage3 = Stage.builder()
//					.stageName("Stage 12")
//					.description("Description of stage 1 for project 2")
//					.project(project2)
//					.order(1)
//					.build();
//
//			Stage stage4 = Stage.builder()
//					.stageName("Stage 22")
//					.description("Description of stage 2 for project 2")
//					.project(project2)
//					.order(2)
//					.build();
//
//			if (stageDb.findAll().isEmpty()) {
//				stageDb.saveAll(List.of(stage1, stage2, stage3, stage4));
//			}
//
//			Task task1 = Task.builder()
//					.taskName("Task 111")
//					.description("Task")
//					.stage(stage1)
//					.
		};
	}


}
