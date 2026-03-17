package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubTaskServiceImpl implements SubTaskService{

    @Override
    public ResponseEntity<?> getAllSubTask(int page, int size, String taskId, LocalDate startDate, LocalDate endDate) {
        var baseResponseDTO = new BaseResponseDTO<SubTaskResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses subtask berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama project member nya apakah masuk ke project tsb.
         => Implementasikan fungsi repository findSubTaskByTaskId dan findSubTaskByTaskIdAndDueDate (untuk Filtering)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> searchSubTask(int page, int size, String taskId, String query) {
        var baseResponseDTO = new BaseResponseDTO<SubTaskResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses subtask pada task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama project member nya apakah masuk ke project tsb.
         => Implementasikan fungsi repository searchSubTaskByQuery
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi juga apakah username untuk project manager berhak menambahkan sub task pada task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Implementasikan mekanisme untuk add sub task baru (Sub Task Id nya bisa pake func generateSubTaskId)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */
        return null;
    }

    @Override
    public ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak update sub task berdasarkan sub task id tsb (
            Contohnya ada di service project sama stage)
         => Validasi project managernya tinggal ambil objek sub task berdasarkan sub task id, ambil task dari sub task,
            dan ambil stage dari task, ambil project dari stage. Dari objek project tinggal cek aja username project
            managernya siapa.
         => Validasi apakah username untuk project member berhak update sub task berdasarkan sub task id tsb (
            Contohnya ada di service project sama stage)
         => Validasi project membernya tinggal ambil objek sub task berdasarkan sub task id.
            Dari objek sub task tinggal cek aja username project membernya siapa.
         => Implementasikan mekanisme untuk update sub task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> getDetailSubTask(String subTaskId) {
        var baseResponseDTO = new BaseResponseDTO<SubTaskDetailResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager dan project member berhak akses sub task berdasarkan sub task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya sama project membernya apakah masuk ke project tsb.
         => Implementasikan mekanisme untuk cek detail sub task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<SubTaskDetailResponseDTO>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> deleteSelectedSubTask(String taskId, List<DeleteRequestDTO> requestDTOList) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak delete sub task berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Implementasikan mekanisme untuk delete lebih dari satu sub task
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */

        return null;
    }

    @Override
    public ResponseEntity<?> reorderSubTask(String taskId, List<ReorderRequestDTO> requestDTOList) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        /**TODO:
         => Validasi apakah username untuk project manager berhak reorder sub task berdasarkan task id tsb (
            Contohnya ada di service project sama stage)
         => Validasinya tinggal ambil objek task berdasarkan task id, ambil stage dari task, dan ambil project dari stage.
            Dari objek project tinggal cek aja username project managernya siapa.
         => Implementasikan mekanisme untuk reorder task (Mekanismenya sama kayak update sub task cuman
            klo ini yang diupdate order nya aja)
         => Kembaliannya itu ResponseEntity<BaseResponseDTO<CrudResponseDTO>>
         */
        return null;
    }

    private String generateSubTaskId(){
        return "STSK" + "-" + String.valueOf(System.currentTimeMillis());
    }
}
