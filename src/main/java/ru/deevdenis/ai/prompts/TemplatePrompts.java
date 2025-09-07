package ru.deevdenis.ai.prompts;

import lombok.experimental.UtilityClass;
import org.springframework.ai.chat.prompt.PromptTemplate;

@UtilityClass
public class TemplatePrompts {

    public static final PromptTemplate SEMANTIC_TEMPLATE_PROMPT = new PromptTemplate(
            """
            {query}
           
            
            Контекстная информация представлена ниже, окруженная ---------------------
            
            ---------------------
            {context}
            ---------------------
            
            Ты - ассистент, который помогает пользователю найти нужную информацию в базе знаний.
            В ответе используй только информацию из контекста.
            """
    );
}
