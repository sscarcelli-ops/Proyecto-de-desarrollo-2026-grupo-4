import { palette } from '../../app/theme';

/**
 * Logo de la plataforma: escudo con tres casas. Sobre fondo oscuro (menú lateral, garita) el escudo va claro;
 * sobre fondo claro, con el azul de la marca.
 */
export function Logo({ size, sobreOscuro = false }: { size: number; sobreOscuro?: boolean }) {
  const escudo = sobreOscuro ? palette.sidebarTextActive : palette.primary;
  const casas = sobreOscuro ? palette.sidebar : palette.surface;

  return (
    <svg
      viewBox="20 14 160 172"
      width={size}
      height={size}
      role="img"
      aria-label="Logo: escudo con tres casas"
      style={{ flexShrink: 0 }}
    >
      <path d="M100 20C84 32 58 42 28 44C26 100 46 150 100 180C154 150 174 100 172 44C142 42 116 32 100 20Z" fill={escudo} />
      <g transform="translate(0 -8)">
        <g fill={casas}>
          <path d="M54 114L66 102L78 114V136H54Z" />
          <path d="M122 114L134 102L146 114V136H122Z" />
          <path d="M100 64L119 84V136H81V84Z" />
          <rect x="54" y="136" width="92" height="6" />
        </g>
        <g fill={escudo}>
          <rect x="93" y="114" width="14" height="22" />
          <rect x="95" y="90" width="10" height="10" />
          <rect x="62" y="119" width="8" height="8" />
          <rect x="130" y="119" width="8" height="8" />
        </g>
      </g>
    </svg>
  );
}
