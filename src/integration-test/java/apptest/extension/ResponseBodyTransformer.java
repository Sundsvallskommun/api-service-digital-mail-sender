package apptest.extension;

import static wiremock.com.github.jknack.handlebars.internal.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class ResponseBodyTransformer extends ResponseDefinitionTransformer {
    
    private final List<Function<String, String>> modifiers = new ArrayList<>();
    
    public ResponseBodyTransformer withModifier(final Function<String, String> modifier) {
        modifiers.add(modifier);
        return this;
    }
    
    @Override
    public String getName() {
        return "response-template";
    }
    
    @Override
    public boolean applyGlobally() {
        return false;
    }
    
    /**
     * The response from skatteverket will contain the url to where we should send the digital mail.
     * Since the response will need to contain the port for wiremock we need to set it here and then transform the url-part with the url and port towards wiremock.
     */
    @Override
    public ResponseDefinition transform(final Request request, final ResponseDefinition responseDefinition, final FileSource files, final Parameters parameters) {
        var body = responseDefinition.getBody();
        if (isBlank(body)) {
            if (isBlank(responseDefinition.getBodyFileName())) {
                throw new IllegalStateException("Either 'body' or 'bodyFileName' must be set");
            }

            body = files.getTextFileNamed(responseDefinition.getBodyFileName()).readContentsAsString();
        }

        // Apply the modifiers, if any
        for (var modifier : modifiers) {
            body = modifier.apply(body);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .but()
            .withBody(body)
            .build();
    }
}
