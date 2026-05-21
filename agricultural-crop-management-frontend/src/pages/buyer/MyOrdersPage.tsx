import { Navigate, useLocation } from "react-router-dom";

export function MyOrdersPage() {
  const location = useLocation();
  return (
    <Navigate
      to={{
        pathname: "/marketplace/orders",
        search: location.search,
      }}
      replace
    />
  );
}

