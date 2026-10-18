package com.bookmyshow.modules.booking;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.modules.booking.BookingTypes.ResaleListing;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest({ResaleController.class, BookingAdminController.class})
class ResaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResaleService resaleService;

    @MockitoBean
    private RefundService refundService;

    @Test
    void listingsArePublic() throws Exception {
        when(resaleService.listings(50L))
                .thenReturn(List.of(new ResaleListing(9L, 50L, List.of("A1"), new BigDecimal("200.00"), "INR", false)));

        mockMvc.perform(get("/api/v1/shows/50/resale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price").value(200.00));
    }

    @Test
    void listingNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/9/resale")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/resale/9/checkout")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminRefunds() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/9/refund").with(TestLogins.as(2L, Roles.THEATRE_ADMIN)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/bookings/9/refund").with(TestLogins.as(1L, Roles.ADMIN)))
                .andExpect(status().isOk());
    }
}
