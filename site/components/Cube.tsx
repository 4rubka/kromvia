export function Cube({ size = 40, tones = ["#c9b5ff", "#8b5cf6", "#2f55c8"], style, className }: { size?: number; tones?: [string, string, string]; style?: React.CSSProperties; className?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 20 20" className={className} style={style} aria-hidden="true">
      <path d="M10 1.5L18 6L10 10.5L2 6Z" fill={tones[0]} />
      <path d="M2 6L10 10.5V19.5L2 15Z" fill={tones[1]} />
      <path d="M18 6L10 10.5V19.5L18 15Z" fill={tones[2]} />
    </svg>
  );
}
