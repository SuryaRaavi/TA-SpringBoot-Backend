package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    @Autowired
    private ProjectManagerDb projectManagerDb;

    @Autowired
    private ProjectMemberDb projectMemberDb;

    @Autowired
    private TaskDb taskDb;

    @Autowired
    private ProjectDb projectDb;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private MemberInProjectDb memberInProjectDb;


    @Override
    public ResponseEntity<?> getAllTask(int page, int size, String stageId, LocalDate startDate, LocalDate endDate) {

        var baseResponseDTO = new BaseResponseDTO<Page<TaskResponseDTO>>();
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses task berdasarkan stage id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama project member nya apakah masuk ke project tsb.
         => Implementasikan fungsi repository findTaskByStageId dan findStageByStageIdAndDueDate (untuk filtering)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<Page<TaskResponseDTO>>>
         */
        return null;
    }

    @Override
    public ResponseEntity<?> searchTask(int page, int size, String stageId, String query) {
        var baseResponseDTO = new BaseResponseDTO<Page<TaskResponseDTO>>();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses task berdasarkan stage id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama project member apakah masuk ke project tsb.
         => Implementasikan fungsi repository searchTaskByQuery
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<Page<TaskResponseDTO>>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi juga apakah username untuk project manager berhak menambahkan task berdasarkan stage id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya apakah masuk ke project tsb.
         => Implementasikan mekanisme untuk add task baru (Task Id nya bisa pake func generateTaskId)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */
        return null;
    }

    @Override
    public ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak update task berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasi project managernya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Validasi apakah username untuk project member berhak update task berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasi project membernya tinggal ambil objek task berdasarkan task id.
            Dari objek task tinggal cek aja username project membernya siapa.
         => Implementasikan mekanisme untuk update task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */

        return null;
    }


    @Override
    public ResponseEntity<?> getDetailTask(String taskId) {
        var baseResponseDTO = new BaseResponseDTO<TaskDetailResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses task berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama apakah project member nya masuk ke project tsb.
         => Implementasikan mekanisme untuk cek detail task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<TaskDetailResponseDTO>>
         */
        return null;
    }

    @Override
    public ResponseEntity<?> deleteSelectedTask(String stageId, List<DeleteRequestDTO> requestDTOList) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak delete task berdasarkan stage id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Implementasikan mekanisme untuk delete lebih dari satu task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> reorderTask(String stageId, List<ReorderRequestDTO> requestDTOList) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak reorder task berdasarkan stage id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek stage berdasarkan stageId, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Implementasikan mekanisme untuk reorder task (Mekanismenya sama kayak update task cuman
            klo ini yang diupdate order nya aja)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */

        return null;
    }

    private String generateTaskId(){
        return "TSK" + "-" + String.valueOf(System.currentTimeMillis());
    }
}
