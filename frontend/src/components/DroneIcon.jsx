export default function DroneIcon({ size = 16 }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 36 36"
            fill="none"
            aria-hidden="true"
        >
            <g
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
            >
                {/* Drone Arms: Draws 4 diagonal lines connecting the center body to the propellers */}
                <path d="m14.5 14.5-7-7m14 7 7-7m-14 14-7 7m14-7 7 7" />
                
                {/* Propellers: 4 ellipses placed at the end of each arm (top-left, top-right, bottom-left, bottom-right) */}
                <ellipse cx="6.5" cy="6.5" rx="4.5" ry="2.6" />
                <ellipse cx="29.5" cy="6.5" rx="4.5" ry="2.6" />
                <ellipse cx="6.5" cy="29.5" rx="4.5" ry="2.6" />
                <ellipse cx="29.5" cy="29.5" rx="4.5" ry="2.6" />
                
                {/* Central Body: Draws the main hexagonal/octagonal chassis of the drone in the middle */}
                <path
                    d="M13 15.5 16 13h4l3 2.5v5L20 23h-4l-3-2.5z"
                    fill="currentColor"
                    stroke="none"
                />
                
                {/* Landing Gear / Camera Mount: 2 small vertical lines at the bottom of the drone body */}
                <path d="M16 23v2m4-2v2" />
                
                {/* Center Detail: A small circle right in the exact middle of the drone body */}
                <circle cx="18" cy="18" r="1.7" fill="#0b1a27" stroke="none" />
            </g>
        </svg>
    );
}