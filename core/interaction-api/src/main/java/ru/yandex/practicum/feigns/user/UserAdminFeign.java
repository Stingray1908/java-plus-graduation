package ru.yandex.practicum.feigns.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.NewUserRequest;
import ru.yandex.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", contextId = "userAdminFeign", path = "/admin/users")
public interface UserAdminFeign {

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("{userId}")
    public UserDto getById(@PathVariable @Positive Long userId);

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("inIds")
    List<UserDto> getAllInIds(@RequestParam List<Long> ids);
}
