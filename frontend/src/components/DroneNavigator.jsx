import { ChevronLeft, ChevronRight } from "lucide-react";
import "../css/DroneNavigator.css";

export default function DroneNavigator({ disabled, onPrevious, onNext }) {
	return (
		<div className="drone-navigator" role="group" aria-label="Drone selection">
			<button
				className="icon-button"
				type="button"
				title="Previous drone"
				aria-label="Previous drone"
				disabled={disabled}
				onClick={onPrevious}
			>
				<ChevronLeft size={18} />
			</button>
			<button
				className="icon-button"
				type="button"
				title="Next drone"
				aria-label="Next drone"
				disabled={disabled}
				onClick={onNext}
			>
				<ChevronRight size={18} />
			</button>
		</div>
	);
}