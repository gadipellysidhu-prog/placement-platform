package com.college.placement.jobintelligence;

import com.college.placement.modules.jobintelligence.dto.ExtractedJobData;
import com.college.placement.modules.jobintelligence.validation.ExtractionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The LLM-output trust boundary: strict parsing, sanitization, structural limits. */
class ExtractionValidatorTest {

    private final ExtractionValidator validator = new ExtractionValidator(new ObjectMapper());

    @Test
    void parsesValidSpecShapedJson() {
        String json = """
                {"title":"Backend Engineer","requiredSkills":["Java","Spring Boot"],
                 "preferredSkills":["Kafka"],"experience":"2+ years","education":"B.E.",
                 "technologies":["Docker"],"frameworks":[],"cloudPlatforms":["AWS"],
                 "softSkills":["Communication"],"salary":"12 LPA","location":"Bengaluru",
                 "employmentType":"Full-time","responsibilities":["Build APIs"],
                 "qualifications":["CS degree"],"confidence":92}
                """;

        ExtractedJobData data = validator.validate(json);

        assertThat(data.title()).isEqualTo("Backend Engineer");
        assertThat(data.requiredSkills()).containsExactly("Java", "Spring Boot");
        assertThat(data.confidence()).isEqualTo(92.0);
        assertThat(data.allSkillMentions())
                .contains("Java", "Spring Boot", "Docker", "AWS", "Kafka", "Communication");
    }

    @Test
    void toleratesMarkdownCodeFences() {
        String fenced = "```json\n{\"requiredSkills\":[\"Python\"]}\n```";

        assertThat(validator.validate(fenced).requiredSkills()).containsExactly("Python");
    }

    @Test
    void rejectsNonJsonCompletions() {
        assertThatThrownBy(() -> validator.validate("Sure! Here are the skills: Java, Python"))
                .isInstanceOf(ExtractionValidator.InvalidExtractionException.class);
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(ExtractionValidator.InvalidExtractionException.class);
    }

    @Test
    void rejectsMissingRequiredSkillsArray() {
        assertThatThrownBy(() -> validator.validate("{\"title\":\"X\"}"))
                .isInstanceOf(ExtractionValidator.InvalidExtractionException.class)
                .hasMessageContaining("requiredSkills");
    }

    @Test
    void stripsAllHtmlFromEveryField() {
        String hostile = """
                {"title":"<script>alert(1)</script>Engineer",
                 "requiredSkills":["<b>Java</b>","<img src=x onerror=alert(1)>SQL"],
                 "responsibilities":["<a href='https://evil'>click</a> build things"]}
                """;

        ExtractedJobData data = validator.validate(hostile);

        assertThat(data.title()).isEqualTo("Engineer");
        assertThat(data.requiredSkills()).containsExactly("Java", "SQL");
        assertThat(data.responsibilities()).containsExactly("click build things");
    }

    @Test
    void dropsOversizedSkillEntriesAndCapsArrays() {
        StringBuilder many = new StringBuilder("{\"requiredSkills\":[");
        for (int i = 0; i < 150; i++) {
            many.append("\"Skill").append(i).append("\",");
        }
        many.append("\"").append("x".repeat(150)).append("\"]}");

        ExtractedJobData data = validator.validate(many.toString());

        assertThat(data.requiredSkills()).hasSizeLessThanOrEqualTo(100);
        assertThat(data.requiredSkills()).allSatisfy(s -> assertThat(s.length()).isLessThanOrEqualTo(100));
    }

    @Test
    void outOfRangeConfidenceIsDiscardedNotFatal() {
        assertThat(validator.validate("{\"requiredSkills\":[],\"confidence\":250}").confidence())
                .isNull();
        assertThat(validator.validate("{\"requiredSkills\":[],\"confidence\":-5}").confidence())
                .isNull();
    }

    @Test
    void unknownJsonFieldsAreIgnored() {
        String extra = "{\"requiredSkills\":[\"Go\"],\"hackedField\":\"ignore me\"}";

        assertThat(validator.validate(extra).requiredSkills()).containsExactly("Go");
    }
}
