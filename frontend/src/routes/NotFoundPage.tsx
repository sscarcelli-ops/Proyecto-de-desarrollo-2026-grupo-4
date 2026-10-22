import { Button, Result } from 'antd';
import { Link } from 'react-router';
import { PATHS } from './paths';

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="La página que buscás no existe."
      extra={
        <Link to={PATHS.home}>
          <Button type="primary">Volver al inicio</Button>
        </Link>
      }
    />
  );
}
