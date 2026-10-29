export default function WorkflowStep({ name, current, done }) {
    return (
        // Main container: Dynamically applies 'current' or 'done' classes based on the step's status
        <div
            className={`workflow-step ${current ? "current" : ""} ${done ? "done" : ""}`}
        >
            {/* Step Marker: The circular icon indicator on the left side */}
            <span className="step-marker">
                {/* Condition: Renders a checkmark (✓) if done, an empty span (for CSS dot/animation) if current, or nothing if pending */}
                {done ? "✓" : current ? <span /> : ""}
            </span>
            
            {/* Step name: Displays the label for this workflow step (e.g., "Takeoff", "Navigate") */}
            <span>{name}</span>
            
            {/* Status Tag: Displays a small "IN PROGRESS" label next to the name if the step is currently active */}
            {current && <small>IN PROGRESS</small>}
        </div>
    );
}