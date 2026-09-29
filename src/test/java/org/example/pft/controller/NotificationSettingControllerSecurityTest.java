package org.example.pft.controller;

import org.example.pft.dto.notification.NotiData;
import org.example.pft.dto.notification.NotificationResponse;
import org.example.pft.dto.notification.NotificationUpdateRequest;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSettingController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class NotificationSettingControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private NotificationResponse notificationResponse;

    @BeforeEach
    void setup() {
        NotiData data = new NotiData();
        data.setDailyReminder(true);
        data.setTipsEnabled(false);
        data.setBudgetAlert(true);

        notificationResponse = new NotificationResponse(
                true,
                "Notification settings fetched successfully",
                data
        );
    }

    @Test
    void showNotificationSetting_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/notifications/settings"));

        verify(notificationService, never()).showNotificationSetting();
    }

    @Test
    @WithMockUser
    void showNotificationSetting_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(notificationService.showNotificationSetting())
                .thenReturn(notificationResponse);

        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification settings fetched successfully"))
                .andExpect(jsonPath("$.data.dailyReminder").value(true))
                .andExpect(jsonPath("$.data.tipsEnabled").value(false))
                .andExpect(jsonPath("$.data.budgetAlert").value(true));

        verify(notificationService).showNotificationSetting();
    }

    @Test
    @WithMockUser
    void showNotificationSetting_whenSettingNotFound_shouldReturn404() throws Exception {
        when(notificationService.showNotificationSetting())
                .thenThrow(new ResourceNotFoundException("Notification setting not found with current User"));

        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification setting not found with current User"));

        verify(notificationService).showNotificationSetting();
    }

    @Test
    void updateNotificationSetting_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "dailyReminder": true,
                    "tipsEnabled": false,
                    "budgetAlert": true
                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/notifications/settings"));

        verify(notificationService, never()).updateNotificationSetting(any(NotificationUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void updateNotificationSetting_withAuthenticatedUser_shouldReturn200() throws Exception {
        NotificationResponse updateResponse = new NotificationResponse(
                true,
                "Notification settings updated successfully",
                notificationResponse.getData()
        );

        when(notificationService.updateNotificationSetting(any(NotificationUpdateRequest.class)))
                .thenReturn(updateResponse);

        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "dailyReminder": true,
                    "tipsEnabled": false,
                    "budgetAlert": true
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification settings updated successfully"))
                .andExpect(jsonPath("$.data.tipsEnabled").value(false));

        ArgumentCaptor<NotificationUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(NotificationUpdateRequest.class);
        verify(notificationService).updateNotificationSetting(requestCaptor.capture());
        assertEquals(true, requestCaptor.getValue().getDailyReminder());
        assertEquals(false, requestCaptor.getValue().getTipsEnabled());
        assertEquals(true, requestCaptor.getValue().getBudgetAlert());
    }

    @Test
    @WithMockUser
    void updateNotificationSetting_withoutTipsEnabled_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "dailyReminder": true,
                    "budgetAlert": true
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("tipsEnabled"))
                .andExpect(jsonPath("$.errors[0].message").value("Tip Enabled is required"));

        verify(notificationService, never()).updateNotificationSetting(any(NotificationUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void updateNotificationSetting_withInvalidTipsEnabledType_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "dailyReminder": true,
                    "tipsEnabled": "abc",
                    "budgetAlert": true
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("tipsEnabled"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be true or false"));

        verify(notificationService, never()).updateNotificationSetting(any(NotificationUpdateRequest.class));
    }

    @Test
    @WithMockUser
    void updateNotificationSetting_whenSettingNotFound_shouldReturn404() throws Exception {
        when(notificationService.updateNotificationSetting(any(NotificationUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Notification setting not found with current User"));

        mockMvc.perform(put("/api/notifications/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "dailyReminder": true,
                    "tipsEnabled": false,
                    "budgetAlert": true
                }
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notification setting not found with current User"));

        verify(notificationService).updateNotificationSetting(any(NotificationUpdateRequest.class));
    }
}
