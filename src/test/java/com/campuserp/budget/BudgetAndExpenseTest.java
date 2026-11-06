package com.campuserp.budget;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class BudgetAndExpenseTest extends ApiTest {

    Department dept;
    Faculty pi;
    Long budget;

    @BeforeEach
    void setUp() throws Exception {
        dept = fx.department("CSE");
        pi = fx.teacher("T1", "Dr. Lead", dept);
        budget = id(postAs(staff(), "/api/budgets",
                "{\"departmentId\":%d,\"financialYear\":\"2026-27\",\"allocated\":100000}".formatted(dept.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPARTMENT")), "$.id");
    }

    Long raise(long amount) throws Exception {
        return id(postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-11-10","amount":%d,"category":"EQUIPMENT","description":"Kit"}"""
                .formatted(budget, amount))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.raisedBy").value("staff")), "$.id");
    }

    @Test
    void onlyApprovedExpensesCountAsUtilized() throws Exception {
        Long e = raise(30000);
        getAs(viewer(), "/api/budgets/" + budget)
                .andExpect(jsonPath("$.utilized").value(0))
                .andExpect(jsonPath("$.pending").value(30000.0))
                .andExpect(jsonPath("$.balance").value(100000.0));

        postAs(staff(), "/api/expenses/" + e + "/approve", "{}").andExpect(status().isForbidden());
        postAs(admin(), "/api/expenses/" + e + "/approve", "{\"remark\":\"ok\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("admin"));

        getAs(viewer(), "/api/budgets/" + budget)
                .andExpect(jsonPath("$.utilized").value(30000.0))
                .andExpect(jsonPath("$.pending").value(0))
                .andExpect(jsonPath("$.balance").value(70000.0))
                .andExpect(jsonPath("$.percentUsed").value(30.0));

        // decided expenses are locked
        putAs(staff(), "/api/expenses/" + e, """
                {"budgetId":%d,"date":"2026-11-10","amount":1,"category":"OTHER","description":"x"}""".formatted(budget))
                .andExpect(status().isConflict());
        deleteAs(staff(), "/api/expenses/" + e).andExpect(status().isConflict());
    }

    @Test
    void overspendNeedsAnExplicitGoAhead() throws Exception {
        postAs(admin(), "/api/expenses/" + raise(60000) + "/approve", "{}").andExpect(status().isOk());
        Long big = raise(50000);
        postAs(admin(), "/api/expenses/" + big + "/approve", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("over its allocation by ₹10,000")));
        postAs(admin(), "/api/expenses/" + big + "/approve", "{\"allowOverspend\":true}").andExpect(status().isOk());
        getAs(viewer(), "/api/budgets/" + budget)
                .andExpect(jsonPath("$.overspent").value(true))
                .andExpect(jsonPath("$.percentUsed").value(110.0));
    }

    @Test
    void rejectingNeedsARemarkAndDoesntCount() throws Exception {
        Long e = raise(20000);
        postAs(admin(), "/api/expenses/" + e + "/reject", "{}").andExpect(status().isConflict());
        postAs(admin(), "/api/expenses/" + e + "/reject", "{\"remark\":\"Wrong budget\"}")
                .andExpect(jsonPath("$.status").value("REJECTED"));
        getAs(viewer(), "/api/budgets/" + budget)
                .andExpect(jsonPath("$.utilized").value(0))
                .andExpect(jsonPath("$.pending").value(0));
        getAs(viewer(), "/api/expenses?status=REJECTED").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void viewerCantRaiseAndDuplicateDepartmentBudgetIsRefused() throws Exception {
        postAs(viewer(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-11-10","amount":10,"category":"OTHER","description":"x"}""".formatted(budget))
                .andExpect(status().isForbidden());
        postAs(staff(), "/api/budgets",
                "{\"departmentId\":%d,\"financialYear\":\"2026-27\",\"allocated\":5}".formatted(dept.getId()))
                .andExpect(status().isConflict());
        postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-11-10","amount":0,"category":"OTHER","description":"x"}""".formatted(budget))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void projectGetsItsOwnBudgetAndShowsUtilizedAndBalance() throws Exception {
        String project = """
                {"title":"Soil sensors","fundingAgency":"DST","sanctionedAmount":500000,"sanctionDate":"2026-06-10",
                 "status":"ONGOING","team":[{"facultyId":%d,"role":"PI"}]}""".formatted(pi.getId());
        var created = postAs(staff(), "/api/projects", project).andExpect(status().isCreated());
        Long projectId = id(created, "$.id");
        Long projectBudget = id(created, "$.budgetId");

        getAs(viewer(), "/api/budgets/" + projectBudget)
                .andExpect(jsonPath("$.type").value("PROJECT"))
                .andExpect(jsonPath("$.allocated").value(500000.0))
                .andExpect(jsonPath("$.financialYear").value("2026-27"));

        Long e = id(postAs(staff(), "/api/expenses", """
                {"budgetId":%d,"date":"2026-08-14","amount":120000,"category":"EQUIPMENT","description":"Sensors"}"""
                .formatted(projectBudget)), "$.id");
        postAs(admin(), "/api/expenses/" + e + "/approve", "{}").andExpect(status().isOk());

        getAs(viewer(), "/api/projects/" + projectId)
                .andExpect(jsonPath("$.utilized").value(120000.0))
                .andExpect(jsonPath("$.balance").value(380000.0))
                .andExpect(jsonPath("$.percentUsed").value(24.0));
        getAs(viewer(), "/api/faculty/" + pi.getId() + "/projects").andExpect(jsonPath("$", hasSize(1)));
        getAs(viewer(), "/api/faculty").andExpect(jsonPath("$[?(@.employeeId=='T1')].projectCount", hasItem(1)));

        // raising the sanctioned amount moves the budget with it
        putAs(staff(), "/api/projects/" + projectId, project.replace("500000", "600000")).andExpect(status().isOk());
        getAs(viewer(), "/api/budgets/" + projectBudget).andExpect(jsonPath("$.allocated").value(600000.0));

        // with expenses on its budget the project can't be deleted
        deleteAs(staff(), "/api/projects/" + projectId).andExpect(status().isConflict());
    }

    @Test
    void projectNeedsExactlyOnePI() throws Exception {
        postAs(staff(), "/api/projects", """
                {"title":"X","fundingAgency":"DST","sanctionedAmount":1,"sanctionDate":"2026-06-10",
                 "status":"ONGOING","team":[{"facultyId":%d,"role":"CO_INVESTIGATOR"}]}""".formatted(pi.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("exactly one Principal Investigator")));
    }

    @Test
    void eventBudgetFollowsTheEvent() throws Exception {
        String event = """
                {"name":"TechFest","type":"festival","startDate":"2026-10-09","endDate":"2026-10-11",
                 "status":"PLANNED","budgetAmount":%s}""";
        Long eventId = id(postAs(staff(), "/api/events", event.formatted("250000"))
                .andExpect(jsonPath("$.budget.allocated").value(250000.0))
                .andExpect(jsonPath("$.type").value("FESTIVAL")), "$.id");
        putAs(staff(), "/api/events/" + eventId, event.formatted("0"))
                .andExpect(jsonPath("$.budget").doesNotExist());
        getAs(viewer(), "/api/budgets?type=EVENT").andExpect(jsonPath("$", hasSize(0)));
    }
}
