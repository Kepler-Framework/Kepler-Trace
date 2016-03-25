package com.kepler.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @example
 * <pre>
 * {@literal @}Service(version = "didimall-transaction-0.0.1", autowired = true)
 * {@literal @}TraceLogger(logger = "com.didichuxing.mall.transaction.impl.TransactionServiceImpl")
 * public class TransactionServiceImpl implements TransactionService {
 * 	{@literal @}TraceEnabled(when = "${printCondition0}")
 * 	{@literal @}Override
 * 	public BuyRecord exchange(String phone, long uid, int productId, String guid) throws TransactionException {
 * 		return null;
 * 	}
 * 	//方法上面的TraceLogger会覆盖class的TraceLogger
 *	{@literal @}TraceLogger(logger = "com.didichuxing.mall.transaction.impl.TransactionServiceImpl.getBuyRecordCnt")
 * 	{@literal @}TraceEnabled(when = "${printCondition1}")
 * 	{@literal @}Override
 * 	public int getBuyRecordsCnt(long uid) throws TransactionException {
 * 		return 0;
 * 	}
 * } 
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TraceLogger {

	/**
	 * 
	 * @return 日志名
	 */
	String logger();
}
