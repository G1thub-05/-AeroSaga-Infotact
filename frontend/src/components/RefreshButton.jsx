import { RotateCcw } from "lucide-react";
import "../css/RefreshButton.css";

export default function RefreshButton({ refreshing, onClick }) {
	return (
		<button
			className="icon-button refresh-button"
			type="button"
			title="Refresh fleet data"
			aria-label="Refresh fleet data"
			aria-busy={refreshing}
			disabled={refreshing}
			onClick={onClick}
		>
			<RotateCcw
				size={15}
				className={refreshing ? "refresh-icon is-refreshing" : "refresh-icon"}
			/>
		</button>
	);
}