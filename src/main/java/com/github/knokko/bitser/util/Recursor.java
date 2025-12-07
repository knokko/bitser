package com.github.knokko.bitser.util;


import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;

public class Recursor<Context, Info> {

	public static <Context, Info> void run(Context context, Info info, SimpleJob<Recursor<Context, Info>> rootMethod) {
		compute(context, info, recursor -> {
			rootMethod.run(recursor);
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	public static <Context, Info, Output> Output compute(
			Context context, Info info, ResultJob<Recursor<Context, Info>, Output> rootMethod
	) {
		Output rootResult;
		Stack<Recursor<Context, Info>> stack = new Stack<>();
		{
			Recursor<Context, Info> rootRecursor = new Recursor<>("Recursor Root", info, stack);
			try {
				rootResult = rootMethod.run(rootRecursor);
			} catch (Throwable jobFailed) {
				throw new RecursorException(createDebugInfoStack(stack, "root"), jobFailed);
			}
			stack.add(rootRecursor);
		}

		while (!stack.isEmpty()) {
			Recursor<Context, Info> methodContext = stack.peek();
			if (methodContext.jobs.isEmpty()) {
				stack.pop();
			} else {
				Entry nextJob = methodContext.jobs.poll();
				if (nextJob instanceof FlatEntry) {
					FlatEntry<Context, Object> flatJob = (FlatEntry<Context, Object>) nextJob;
					try {
						flatJob.output.output = flatJob.job.run(context);
						flatJob.output.completed = true;
					} catch (Throwable jobFailed) {
						throw new RecursorException(createDebugInfoStack(stack, nextJob.debugInfo), jobFailed);
					}
				} else if (nextJob instanceof NestedEntry) {
					Recursor<Context, Info> nextContext = new Recursor<>(nextJob.debugInfo, info, stack);
					NestedEntry<Context, Info, Object> nestedJob = (NestedEntry<Context, Info, Object>) nextJob;
					try {
						nestedJob.output.output = nestedJob.job.run(nextContext);
						nestedJob.output.completed = true;
					} catch (Throwable jobFailed) {
						throw new RecursorException(createDebugInfoStack(stack, nextJob.debugInfo), jobFailed);
					}
					stack.add(nextContext);
				}
			}
		}

		return rootResult;
	}

	private static <Context, Info> String createDebugInfoStack(Stack<Recursor<Context, Info>> stack, String last) {
		StringBuilder debugInfo = new StringBuilder(10 * stack.size() + last.length());
		for (Recursor<Context, Info> method : stack) debugInfo.append(method.debugInfo).append(" -> ");
		debugInfo.append(last);
		return debugInfo.toString();
	}

	private final String debugInfo;
	public final Info info;
	private final Queue<Entry> jobs = new ArrayDeque<>();
	private final Stack<Recursor<Context, Info>> stack;

	private Recursor(String debugInfo, Info info, Stack<Recursor<Context, Info>> stack ) {
		this.debugInfo = debugInfo;
		this.info = info;
		this.stack = stack;
	}

	public <Output> JobOutput<Output> computeFlat(String debugInfo, ResultJob<Context, Output> job) {
		JobOutput<Output> output = new JobOutput<>();
		jobs.add(new FlatEntry<>(debugInfo, job, output));
		return output;
	}

	public void runFlat(String debugInfo, SimpleJob<Context> job) {
		computeFlat(debugInfo, context -> {
			job.run(context);
			return null;
		});
	}

	public <Output> JobOutput<Output> computeNested(String debugInfo, ResultJob<Recursor<Context, Info>, Output> job) {
		JobOutput<Output> output = new JobOutput<>();
		jobs.add(new NestedEntry<>(debugInfo, job, output));
		return output;
	}

	public void runNested(String debugInfo, SimpleJob<Recursor<Context, Info>> job) {
		computeNested(debugInfo, nested -> {
			job.run(nested);
			return null;
		});
	}

	public String createDebugInfoStack(String lastInfo) {
		return createDebugInfoStack(stack, lastInfo);
	}

	private static class Entry {

		final String debugInfo;

		Entry(String debugInfo) {
			this.debugInfo = debugInfo;
		}
	}

	private static class FlatEntry<Context, Output> extends Entry {

		final ResultJob<Context, Output> job;
		final JobOutput<Output> output;

		FlatEntry(String debugInfo, ResultJob<Context, Output> job, JobOutput<Output> output) {
			super(debugInfo);
			this.job = job;
			this.output = output;
		}
	}

	private static class NestedEntry<Context, Info, Output> extends Entry {

		final ResultJob<Recursor<Context, Info>, Output> job;
		final JobOutput<Output> output;

		NestedEntry(String debugInfo, ResultJob<Recursor<Context, Info>, Output> job, JobOutput<Output> output) {
			super(debugInfo);
			this.job = job;
			this.output = output;
		}
	}

	@FunctionalInterface
	public interface SimpleJob<Input> {
		void run(Input input) throws Throwable;
	}

	@FunctionalInterface
	public interface ResultJob<Input, Output> {
		Output run(Input input) throws Throwable;
	}
}
