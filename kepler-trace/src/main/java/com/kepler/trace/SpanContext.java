package com.kepler.trace;

public class SpanContext {

	private static final ThreadLocal<Span> span = new ThreadLocal<Span>() {

		@Override
		protected Span initialValue() {
			return new Span();
		}
		
	};
	
	public static void set(Span span) {
		SpanContext.span.set(span);
	}
	
	public static Span get() {
		return SpanContext.span.get();
	}
	
}
