package ru.deevdenis.ai.rag;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import ru.deevdenis.ai.properties.AiProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.deevdenis.ai.prompts.TemplatePrompts.RU_SEMANTIC_TEMPLATE_PROMPT;

/**
 * Context for the question is retrieved from a Vector Store and added to the prompt's user text.
 */
public class RedisMemoryAdvisor implements CallAdvisor, Ordered {

    public static final String RETRIEVED_DOCUMENTS = "redis_retrieved_documents";
    public static final String FILTER_EXPRESSION = "redis_filter_expression";

    private static final int DEFAULT_ORDER = 0;

    private final AiProperties aiProperties;
    private final SearchRequest searchRequest;
    private final PromptTemplate promptTemplate;
    private final SemanticCache semanticCache;
    private final int order;

    public RedisMemoryAdvisor(AiProperties aiProperties, SemanticCache semanticCache) {
        this(SearchRequest.builder().build(), RU_SEMANTIC_TEMPLATE_PROMPT, DEFAULT_ORDER, aiProperties, semanticCache);
    }

    public RedisMemoryAdvisor(PromptTemplate promptTemplate, AiProperties aiProperties, SemanticCache semanticCache) {
        this(SearchRequest.builder().build(), promptTemplate, DEFAULT_ORDER, aiProperties, semanticCache);
    }

    public RedisMemoryAdvisor(
            SearchRequest searchRequest,
            @Nullable PromptTemplate promptTemplate,
            int order,
            AiProperties aiProperties,
            SemanticCache semanticCache
    ) {
        Assert.notNull(searchRequest, "Search request must not be null");

        this.searchRequest = searchRequest;
        this.promptTemplate = Objects.nonNull(promptTemplate) ? promptTemplate : RU_SEMANTIC_TEMPLATE_PROMPT;
        this.order = order;
        this.aiProperties = aiProperties;
        this.semanticCache = semanticCache;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Nullable
    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        if (!context.containsKey(FILTER_EXPRESSION)
                || !StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
            return this.searchRequest.getFilterExpression();
        }
        return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        // 1. Search for similar documents in the vector store.
        SearchRequest localSearchRequest = SearchRequest.from(this.searchRequest)
                .query(chatClientRequest.prompt().getUserMessage().getText())
                .topK(aiProperties.getSimilarity().getTopK())
                .filterExpression(doGetFilterExpression(chatClientRequest.context()))
                .build();

        String semanticResponse = semanticCache.findSimilarityRequest(localSearchRequest);

        if (StringUtils.hasText(semanticResponse)) {
            // 2. Create the context from the documents
//            Map<String, Object> context = new HashMap<>(chatClientRequest.context());
//            context.put(RETRIEVED_DOCUMENTS, semanticResponse);
//
//            // 3. Augment the user prompt with the document context.
//            UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
//            String augmentedUserText = this.promptTemplate
//                    .render(Map.of("query", userMessage.getText(), "context", semanticResponse));
//
//            // 4. Update ChatClientRequest with augmented prompt.
//            ChatClientRequest query = chatClientRequest.mutate()
//                    .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
//                    .context(context)
//                    .build();

            return ChatClientResponse.builder()
                    .chatResponse(ChatResponse.builder()
                            .generations(List.of(new Generation(new AssistantMessage(semanticResponse))))
                            .build())
                    .context(Map.copyOf(chatClientRequest.context()))
                    .build();

//            return chain.nextCall(query);
        }

        // 5. No documents found, continue with the original request.
        return chain.nextCall(chatClientRequest);
    }
}
