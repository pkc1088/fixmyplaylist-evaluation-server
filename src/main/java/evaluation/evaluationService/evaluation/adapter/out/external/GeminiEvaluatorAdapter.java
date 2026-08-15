package evaluation.evaluationService.evaluation.adapter.out.external;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import evaluation.evaluationService.evaluation.application.port.out.EvaluateRecoveryPort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiEvaluatorAdapter implements EvaluateRecoveryPort {

    private final JudgeAiService judgeService;


    public GeminiEvaluatorAdapter(ChatModel chatModel) {
        this.judgeService = AiServices.create(JudgeAiService.class, chatModel);
    }

    interface JudgeAiService {
        @SystemMessage("""
                You are a strict evaluator for YouTube music recoveries.
                You must return ONLY a JSON object with 'label' (SUCCESS or FAIL), 'confidence' (float).
                
                CRITICAL RULES FOR JSON FIELDS:
                1. 'label': Must be exactly "SUCCESS" or "FAIL". You MUST always select one.
                    - SUCCESS: The recovered video is an appropriate replacement.
                    - FAIL: The recovered video is not an appropriate replacement.
                
                2. 'confidence': Must be a float strictly between 0.0 and 1.0. It represents how confident you are that the selected 'label' is correct.
                    - 0.9–1.0: Very high confidence; the evidence strongly supports the selected label.
                    - 0.7–0.89: High confidence; the selected label is well supported, with minor uncertainty.
                    - 0.4–0.69: Moderate confidence; the selected label is plausible, but significant uncertainty remains.
                    - 0.0–0.39: Low confidence; the evidence is insufficient to confidently distinguish between SUCCESS and FAIL.
                """)
        @UserMessage("""
                Current recovery:
                Target video: {{target}}
                Recovered video: {{source}}
                
                Below are previous recovery cases that were reviewed by humans.
                Use them as reference examples, but make your own judgment.
                
                {{examples}}
                """)
        EvaluationResult evaluateWithRag(
                @V("target") String target,
                @V("source") String source,
                @V("examples") String examples
        );
    }

    public EvaluationResult evaluateWithRag(EvaluationCase testCase, List<RetrievedCase> similarCases) {
        String examples = similarCases.stream()
                .map(this::formatExample)
                .collect(Collectors.joining("\n\n"));

        return judgeService.evaluateWithRag(
                testCase.getTargetTitle(),
                testCase.getSourceTitle(),
                examples
        );
    }

    private String formatExample(RetrievedCase retrievedCase) {
        ReferenceCase example = retrievedCase.referenceCase();
        return """
                Target: %s
                Source: %s
                Human Label: %s
                Human Reason: %s
                """.formatted(
                example.getTargetTitle(),
                example.getSourceTitle(),
                example.getHumanLabel(),
                example.getHumanReason()
        );
    }
}