package com.kepler.trace.impl;

import org.apache.commons.logging.Log;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.kepler.org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author zhangjiehao 2016年1月12日
 */
public class TraceConfig {

	private final ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private Log logger;

	private String traceCondition;

	private Expression expression;

	public Log getLogger() {
		return this.logger;
	}

	public void setLogger(Log logger) {
		this.logger = logger;
	}

	public String getTraceCondition() {
		return this.traceCondition;
	}

	public void setTraceCondition(String traceCondition) {
		this.traceCondition = traceCondition;
		this.expression = this.parser.parseExpression(traceCondition);
	}

	public Expression getExpression() {
		return this.expression;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
