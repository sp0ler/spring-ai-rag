package ru.deevdenis.ai.prompts;

import lombok.experimental.UtilityClass;
import org.springframework.ai.chat.prompt.PromptTemplate;

@UtilityClass
public class TemplatePrompts {

    public static final PromptTemplate RU_SEMANTIC_TEMPLATE_PROMPT = new PromptTemplate("""
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

    public static final PromptTemplate EN_SEMANTIC_TEMPLATE_PROMPT = new PromptTemplate("""
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
}
