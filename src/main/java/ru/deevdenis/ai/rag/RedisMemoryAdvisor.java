package ru.deevdenis.ai.rag;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Context for the question is retrieved from a Vector Store and added to the prompt's user text.
 */
public class RedisMemoryAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "redis_retrieved_documents";

    public static final String FILTER_EXPRESSION = "redis_filter_expression";

    public static final PromptTemplate RU_DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
            {query}
            
            Контекстная информация представлена ниже, окруженная ---------------------
            
            ---------------------
            {context}
            ---------------------
            
            Учитывая контекст и предоставленную информацию об истории вопроса, а не предыдущие знания,
            ответьте на комментарий пользователя. Если ответ не соответствует контексту, сообщите
            пользователю, что вы не можете ответить на вопрос.
            """
    );

    public static final PromptTemplate EN_DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			{query}

			Context information is below, surrounded by ---------------------

			---------------------
			{context}
			---------------------

			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			"""
    );

    private static final int DEFAULT_ORDER = 0;

    private final RedisVectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final PromptTemplate promptTemplate;
    private final int order;

    public RedisMemoryAdvisor(RedisVectorStore vectorStore) {
        this(vectorStore, SearchRequest.builder().build(), RU_DEFAULT_PROMPT_TEMPLATE, DEFAULT_ORDER);
    }

    public RedisMemoryAdvisor(RedisVectorStore vectorStore, PromptTemplate promptTemplate) {
        this(vectorStore, SearchRequest.builder().build(), promptTemplate, DEFAULT_ORDER);
    }

    public RedisMemoryAdvisor(
            RedisVectorStore vectorStore,
            SearchRequest searchRequest,
            @Nullable PromptTemplate promptTemplate,
            int order
    ) {
        Assert.notNull(vectorStore, "Vector store must not be null");
        Assert.notNull(searchRequest, "Search request must not be null");

        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.promptTemplate = Objects.nonNull(promptTemplate) ? promptTemplate : RU_DEFAULT_PROMPT_TEMPLATE;
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 1. Search for similar documents in the vector store.
        SearchRequest localSearchRequest = SearchRequest.from(this.searchRequest)
                .query(chatClientRequest.prompt().getUserMessage().getText())
                .filterExpression(doGetFilterExpression(chatClientRequest.context()))
                .build();

        List<Document> documents = vectorStore.similaritySearch(localSearchRequest);

        // 2. Create the context from the documents
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, documents);

        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        // 3. Augment the user prompt with the document context.
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String augmentedUserText = this.promptTemplate
                .render(Map.of("query", userMessage.getText(), "context", documentContext));

        // 4. Update ChatClientRequest with augmented prompt.
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        }
        else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
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
}
