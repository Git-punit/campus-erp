package com.campuserp.training;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
import com.campuserp.faculty.Faculty;
import com.campuserp.setup.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class TrainingTest extends ApiTest {

    Long program;

    @BeforeEach
    void setUp() throws Exception {
        Department d = fx.department("ECE");
        Faculty coordinator = fx.teacher("T1", "Coordinator", d);
        program = id(postAs(staff(), "/api/training-programs",
                "{\"name\":\"Industrial IoT\",\"durationDays\":5,\"coordinatorId\":%d}".formatted(coordinator.getId()))
                .andExpect(status().isCreated()), "$.id");
    }

    static String client(boolean proposal, String accepted, boolean conducted) {
        return """
                {"clientName":"Client X","contactPerson":"A Person","dateContacted":"2026-08-01",
                 "proposalSubmitted":%s,"proposalDate":%s,
                 "accepted":%s,"acceptedDate":%s,"acceptedRemarks":"remark for %s",
                 "conducted":%s,"conductedFrom":%s,"conductedTo":%s,
                 "budgetRequired":40000,"feeCharged":60000}"""
                .formatted(proposal, proposal ? "\"2026-08-10\"" : "null",
                        accepted == null ? "null" : "\"" + accepted + "\"",
                        accepted == null ? "null" : "\"2026-08-20\"", accepted,
                        conducted, conducted ? "\"2026-09-01\"" : "null", conducted ? "\"2026-09-05\"" : "null");
    }

    @Test
    void stepsMustGoInOrder() throws Exception {
        String url = "/api/training-programs/" + program + "/clients";
        postAs(staff(), url, client(false, "YES", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("proposal as submitted")));
        postAs(staff(), url, client(true, "NEEDS_REVISION", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("after the client has accepted")));
        postAs(staff(), url, client(false, null, false)).andExpect(status().isCreated());
    }

    @Test
    void acceptingCreatesTheBudgetAndHistoryIsKept() throws Exception {
        Long c = id(postAs(staff(), "/api/training-programs/" + program + "/clients", client(true, null, false))
                .andExpect(jsonPath("$.budgetId").doesNotExist()), "$.id");

        putAs(staff(), "/api/training-clients/" + c, client(true, "NEEDS_REVISION", false))
                .andExpect(jsonPath("$.budgetId").doesNotExist());
        var accepted = putAs(staff(), "/api/training-clients/" + c, client(true, "YES", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[*].accepted", contains("NEEDS_REVISION", "YES")))
                .andExpect(jsonPath("$.history[1].changedBy").value("staff"));
        Long budget = id(accepted, "$.budgetId");

        getAs(viewer(), "/api/budgets/" + budget)
                .andExpect(jsonPath("$.type").value("TRAINING"))
                .andExpect(jsonPath("$.allocated").value(40000.0));

        // saving again without changing "accepted" doesn't add history
        putAs(staff(), "/api/training-clients/" + c, client(true, "YES", true))
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.conducted").value(true));
    }

    @Test
    void profitIsFeeMinusApprovedCost() throws Exception {
        Long c = id(postAs(staff(), "/api/training-programs/" + program + "/clients", client(true, "YES", true)), "$.id");
        Long budget = id(getAs(viewer(), "/api/training-clients/" + c), "$.budgetId");

        Long e1 = id(postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-09-01","amount":25000,"category":"CONSUMABLES","description":"Kits"}"""
                .formatted(budget)), "$.id");
        postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-09-02","amount":5000,"category":"TRAVEL","description":"Bus"}"""
                .formatted(budget));
        postAs(admin(), "/api/expenses/" + e1 + "/approve", "{}").andExpect(status().isOk());

        getAs(viewer(), "/api/training-clients/" + c)
                .andExpect(jsonPath("$.actualCost").value(25000.0))
                .andExpect(jsonPath("$.profit").value(35000.0));
        getAs(viewer(), "/api/training-programs/" + program)
                .andExpect(jsonPath("$.clientsReached").value(1))
                .andExpect(jsonPath("$.proposals").value(1))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.conducted").value(1));
    }

    @Test
    void clientCanChangeTheirMindWhileAnExpenseIsPending() throws Exception {
        Long c = id(postAs(staff(), "/api/training-programs/" + program + "/clients", client(true, "YES", false)), "$.id");
        Long budget = id(getAs(viewer(), "/api/training-clients/" + c), "$.budgetId");
        postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-09-01","amount":5000,"category":"TRAVEL","description":"Site visit"}"""
                .formatted(budget)).andExpect(status().isCreated());

        // used to fail with "already has expenses against it"; now the budget simply stays
        putAs(staff(), "/api/training-clients/" + c, client(true, "NEEDS_REVISION", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value("NEEDS_REVISION"))
                .andExpect(jsonPath("$.budgetId").value(budget.intValue()));
    }
}
