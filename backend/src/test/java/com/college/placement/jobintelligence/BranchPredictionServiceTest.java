package com.college.placement.jobintelligence;

import com.college.placement.modules.jobintelligence.service.BranchPredictionService;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.service.BranchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BranchPredictionServiceTest {

    private BranchService branchService;
    private BranchPredictionService predictionService;

    private static Branch branch(String name, String code) {
        Branch b = new Branch();
        b.setName(name);
        b.setCode(code);
        return b;
    }

    @BeforeEach
    void setUp() {
        branchService = Mockito.mock(BranchService.class);
        Mockito.when(branchService.getAll()).thenReturn(List.of(
                branch("Computer Science", "CSE"),
                branch("Electronics & Communication", "ECE"),
                branch("Mechanical Engineering", "MECH"),
                branch("Civil Engineering", "CIVIL")));
        predictionService = new BranchPredictionService(branchService);
    }

    @Test
    void embeddedSkillsPredictEceAndCs() {
        var prediction = predictionService.predict(List.of("Embedded C", "STM32", "UART", "CAN"));

        assertThat(prediction.matchedBranches())
                .extracting(Branch::getCode)
                .contains("ECE", "CSE");
        assertThat(prediction.predictedNames()).isNotEmpty();
    }

    @Test
    void mechanicalSkillsPredictMechanicalBranch() {
        var prediction = predictionService.predict(List.of("SolidWorks", "ANSYS", "CNC Programming"));

        assertThat(prediction.matchedBranches())
                .extracting(Branch::getCode)
                .contains("MECH");
    }

    @Test
    void softwareSkillsPredictCsBranches() {
        var prediction = predictionService.predict(List.of("Java", "Spring Boot", "AWS"));

        assertThat(prediction.matchedBranches())
                .extracting(Branch::getCode)
                .contains("CSE")
                .doesNotContain("MECH", "CIVIL");
    }

    @Test
    void noSignalsFallBackToSoftwareBranchesOnly() {
        var prediction = predictionService.predict(List.of("Quantum Basket Weaving"));

        assertThat(prediction.predictedNames()).contains("Computer Science");
        assertThat(prediction.matchedBranches())
                .extracting(Branch::getCode)
                .containsExactly("CSE");
    }

    @Test
    void neverInventsBranchesOutsideTheCatalog() {
        Mockito.when(branchService.getAll()).thenReturn(List.of(branch("Computer Science", "CSE")));

        var prediction = predictionService.predict(List.of("STM32", "UART"));

        // ECE is predicted by name but does not exist in this institution's catalog.
        assertThat(prediction.matchedBranches())
                .extracting(Branch::getCode)
                .containsOnly("CSE");
    }
}
