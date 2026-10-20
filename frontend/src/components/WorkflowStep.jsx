export default function WorkflowStep({ name, current, done }) {
	return (
		<div
			className={`workflow-step ${current ? "current" : ""} ${done ? "done" : ""}`}
		>
			<span className="step-marker">
				{done ? "✓" : current ? <span /> : ""}
			</span>
			<span>{name}</span>
			{current && <small>IN PROGRESS</small>}
		</div>
	);
}
