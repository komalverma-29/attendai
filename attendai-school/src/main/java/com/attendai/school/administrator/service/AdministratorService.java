package com.attendai.school.administrator.service;

import com.attendai.school.administrator.dto.AdministratorResponse;
import com.attendai.school.administrator.dto.AdministratorSummaryResponse;
import com.attendai.school.administrator.dto.ChangeAdministratorStatusRequest;
import com.attendai.school.administrator.dto.CreateAdministratorRequest;
import com.attendai.school.administrator.dto.UpdateAdministratorRequest;
import com.attendai.school.administrator.entity.AdministratorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdministratorService {

    AdministratorResponse createAdministrator(Long schoolId, CreateAdministratorRequest request);

    AdministratorResponse findById(Long schoolId, Long id);

    Page<AdministratorSummaryResponse> listAdministrators(Long schoolId,
                                                           AdministratorStatus status,
                                                           Pageable pageable);

    AdministratorResponse updateAdministrator(Long schoolId, Long id,
                                               UpdateAdministratorRequest request);

    AdministratorResponse changeStatus(Long schoolId, Long id,
                                        ChangeAdministratorStatusRequest request);

    void deleteAdministrator(Long schoolId, Long id);
}
