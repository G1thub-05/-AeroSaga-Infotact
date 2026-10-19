import "../css/Metric.css";
export default function Metric({
	icon,
	name,
	value,
	suffix,
	accent = "muted",
}) {
	return (
		<div className="metric">
			<div className={`metric-icon ${accent}`}>{icon}</div>
			<div>
				<span className="metric-label">{name}</span>
				<strong>{value}</strong>
				<small>{suffix}</small>
			</div>
		</div>
	);
}