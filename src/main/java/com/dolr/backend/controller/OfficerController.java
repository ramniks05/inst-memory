package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.repository.OfficerProjection;
import com.dolr.backend.service.OfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.dolr.backend.dto.UpdateOfficerRequest;
import java.util.List;
import com.dolr.backend.dto.ChangePasswordRequest;

@RestController
@RequestMapping("/api/officers")
@RequiredArgsConstructor
public class OfficerController {

    private final OfficerService officerService;

    // ✅ Get ALL officers (for table)
    @GetMapping
    public ApiResponse<List<OfficerProjection>> getAllOfficers() {
        return ApiResponse.ok(officerService.getAllOfficers());
    }

    // ✅ Get officers by division (for dropdown / reporting officer)
    @GetMapping("/by-division")
    public ApiResponse<List<OfficerProjection>> getOfficersByDivision(
            @RequestParam String department,
            @RequestParam String division
    ) {
        return ApiResponse.ok(
                officerService.getOfficersByDivision(department, division)
        );
    }

    // ✅ Update officer
    @PutMapping("/{id}")
    public ApiResponse<Void> updateOfficer(
            @PathVariable Long id,
            @RequestBody UpdateOfficerRequest request
    ) {
        return officerService.updateOfficer(id, request);
    }

    // ✅ Delete officer
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOfficer(
            @PathVariable Long id
    ) {
        return officerService.deleteOfficer(id);
    }

    // ✅ Change password
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestBody ChangePasswordRequest request
    ) {
        return officerService.changePassword(request);
    }
}