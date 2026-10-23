package com.pgmanagement.dto.response;

import com.pgmanagement.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRemovalResponse {

    private Long roomId;
    private String roomNumber;
    private Integer previousOccupancy;
    private Integer currentOccupancy;
    private RoomStatus status;
    private RemovedTenantInfo removedTenant;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemovedTenantInfo {
        private Long userId;
        private String name;
        private String stayDuration;
    }
}
