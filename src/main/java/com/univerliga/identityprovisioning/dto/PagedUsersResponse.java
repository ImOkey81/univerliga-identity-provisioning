package com.univerliga.identityprovisioning.dto;

import java.util.List;

public record PagedUsersResponse(List<UserStatusDto> items, PageInfo page) {
}
