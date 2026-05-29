import { useMemo, useState } from "react";
import { MapPin, MessageCircle, Minus, Package, Plus, ShieldCheck, ShoppingCart, Star, Store } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import {
  useMarketplaceAddToCart,
  useMarketplaceProductDetail,
  useMarketplaceProductReviews,
  useMarketplaceTraceability,
} from "@/features/marketplace/hooks";
import { formatDateTime, formatVnd } from "@/features/marketplace/lib/format";
import "./ProductDetailPage.css";

function StarRating({ rating }: { rating?: number }) {
  const score = Math.min(Math.max(Math.round(rating ?? 5), 1), 5);
  return (
    <div className="pdp__review-stars">
      {Array.from({ length: 5 }, (_, index) => (
        <span key={index} className={index < score ? "pdp__review-star--filled" : "pdp__review-star--empty"}>
          ★
        </span>
      ))}
    </div>
  );
}

export function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [quantity, setQuantity] = useState(1);
  const [imageFailed, setImageFailed] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [activeTab, setActiveTab] = useState<"description" | "reviews">("description");
  const { addToCart, isAdding } = useMarketplaceAddToCart();

  const productQuery = useMarketplaceProductDetail(slug);
  const product = productQuery.data;

  const allImages = useMemo(() => {
    if (!product) return [];
    const urls: string[] = [];
    if (product.imageUrls?.length) {
      urls.push(...product.imageUrls);
    } else if (product.imageUrl) {
      urls.push(product.imageUrl);
    }
    return urls;
  }, [product]);

  const primaryImage = allImages[activeImageIndex] ?? null;

  const reviewsQuery = useMarketplaceProductReviews(product?.id, { page: 0, size: 5 });
  const traceabilityQuery = useMarketplaceTraceability(product?.traceable ? product.id : null);

  const canIncrease = useMemo(() => {
    if (!product) {
      return false;
    }
    return quantity < product.availableQuantity;
  }, [product, quantity]);

  const quantityValue = product
    ? Math.min(Math.max(quantity, 1), Math.max(product.availableQuantity, 1))
    : quantity;

  const reviewCount = reviewsQuery.data?.items?.length ?? 0;

  // ── Loading state ───────────────────────────────────────────
  if (productQuery.isLoading) {
    return (
      <div className="pdp__state-box">
        <div className="pdp__state-card">
          Đang tải chi tiết sản phẩm...
        </div>
      </div>
    );
  }

  // ── Error / not-found state ─────────────────────────────────
  if (productQuery.isError || !product) {
    return (
      <div className="pdp__state-box">
        <div className="pdp__state-card pdp__state-card--error">
          <p>Sản phẩm không tồn tại hoặc chưa được công khai.</p>
          <Link to="/marketplace/products">
            Quay lại danh sách sản phẩm
          </Link>
        </div>
      </div>
    );
  }

  const farmInitial = (product.farmName ?? product.farmerDisplayName ?? "F").charAt(0).toUpperCase();

  return (
    <div className="pdp">
      <div className="pdp__container">
        {/* ── Breadcrumb ───────────────────────────────────────── */}
        <nav className="pdp__breadcrumb">
          <Link to="/marketplace">Trang chủ</Link>
          <span className="pdp__breadcrumb-sep">›</span>
          <Link to="/marketplace/products">Sản phẩm</Link>
          <span className="pdp__breadcrumb-sep">›</span>
          {product.category && (
            <>
              <Link to={`/marketplace/products?category=${encodeURIComponent(product.category)}`}>
                {product.category}
              </Link>
              <span className="pdp__breadcrumb-sep">›</span>
            </>
          )}
          <span className="pdp__breadcrumb-current">{product.name}</span>
        </nav>

        {/* ═══ MAIN 2-COLUMN LAYOUT ═══════════════════════════ */}
        <div className="pdp__main">
          {/* ── Image Gallery (left) ──────────────────────────── */}
          <div className="pdp__gallery">
            <div className="pdp__gallery-main">
              {primaryImage && !imageFailed ? (
                <img
                  src={primaryImage}
                  alt={product.name}
                  referrerPolicy="no-referrer"
                  onError={() => setImageFailed(true)}
                />
              ) : (
                <div className="pdp__gallery-fallback">
                  <Package strokeWidth={1.5} />
                  <span>Không có hình ảnh</span>
                </div>
              )}
              {allImages.length > 1 && (
                <span className="pdp__gallery-counter">
                  {activeImageIndex + 1} / {allImages.length}
                </span>
              )}
            </div>

            {allImages.length > 1 && (
              <div className="pdp__gallery-thumbs">
                {allImages.map((url, index) => (
                  <button
                    key={index}
                    type="button"
                    className={`pdp__gallery-thumb${index === activeImageIndex ? " pdp__gallery-thumb--active" : ""}`}
                    onClick={() => {
                      setActiveImageIndex(index);
                      setImageFailed(false);
                    }}
                  >
                    <img src={url} alt={`${product.name} ${index + 1}`} referrerPolicy="no-referrer" />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* ── Right Info Panel ──────────────────────────────── */}
          <div className="pdp__info">
            {/* Badges */}
            <div className="pdp__badges">
              <span className="pdp__category-badge">{product.category}</span>
              {product.traceable && (
                <span className="pdp__trace-badge">
                  <ShieldCheck /> Có truy xuất
                </span>
              )}
            </div>

            {/* Product name */}
            <h1 className="pdp__name">{product.name}</h1>

            {/* Rating / reviews / sold */}
            <div className="pdp__rating-row">
              <div className="pdp__rating-stars">
                <Star className="pdp__review-star--filled" size={14} style={{ fill: "#f59e0b" }} />
                <span className="pdp__rating-score">
                  {product.ratingAverage ? product.ratingAverage.toFixed(1) : "5.0"}
                </span>
              </div>
              <span className="pdp__rating-divider">|</span>
              <span>{product.ratingCount} đánh giá</span>
              <span className="pdp__rating-divider">|</span>
              <span>Đã bán {product.ratingCount > 100 ? `${(product.ratingCount / 100).toFixed(1)}k` : product.ratingCount}</span>
            </div>

            {/* Price */}
            <div className="pdp__price-block">
              <span className="pdp__price">
                {formatVnd(product.price)}
                <span className="pdp__price-unit">/ {product.unit}</span>
              </span>
            </div>

            {/* Short description */}
            {product.shortDescription && (
              <p className="pdp__short-desc">{product.shortDescription}</p>
            )}

            {/* Quantity selector */}
            <div className="pdp__qty-section">
              <span className="pdp__qty-label">Số lượng</span>
              {isAuthenticated ? (
                <div className="pdp__qty-row">
                  <div className="pdp__qty-control">
                    <button
                      className="pdp__qty-btn"
                      onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                      type="button"
                    >
                      <Minus />
                    </button>
                    <span className="pdp__qty-value">{quantityValue}</span>
                    <button
                      className="pdp__qty-btn"
                      onClick={() => setQuantity((prev) => prev + 1)}
                      disabled={!canIncrease}
                      type="button"
                    >
                      <Plus />
                    </button>
                  </div>
                  <span className="pdp__qty-stock">{product.availableQuantity} sản phẩm có sẵn</span>
                </div>
              ) : (
                <div className="pdp__auth-notice">
                  Hãy đăng nhập để thêm sản phẩm vào giỏ hàng hoặc đặt mua ngay.
                </div>
              )}
            </div>

            {/* CTA Buttons */}
            <div className="pdp__cta-group">
              {isAuthenticated ? (
                <>
                  <button
                    type="button"
                    className="pdp__cta-btn pdp__cta-btn--cart"
                    disabled={isAdding || product.availableQuantity <= 0}
                    onClick={async () => {
                      await addToCart(product.id, quantity);
                    }}
                  >
                    <ShoppingCart /> Thêm vào giỏ
                  </button>
                  <button
                    type="button"
                    className="pdp__cta-btn pdp__cta-btn--buy"
                    disabled={isAdding || product.availableQuantity <= 0}
                    onClick={async () => {
                      const mode = await addToCart(product.id, quantity);
                      if (mode === "server") {
                        navigate("/marketplace/cart");
                      }
                    }}
                  >
                    {isAdding ? "Đang xử lý..." : "Mua ngay"}
                  </button>
                </>
              ) : (
                <>
                  <Link to="/sign-in" className="pdp__cta-btn pdp__cta-btn--outline">
                    Đăng nhập
                  </Link>
                  <Link to="/sign-up" className="pdp__cta-btn pdp__cta-btn--buy">
                    Tạo tài khoản
                  </Link>
                </>
              )}
            </div>

            {/* ── Farm Card ──────────────────────────────────── */}
            <div className="pdp__farm-card">
              <div className="pdp__farm-card-header">
                <div className="pdp__farm-avatar">{farmInitial}</div>
                <div className="pdp__farm-meta">
                  <div className="pdp__farm-name-row">
                    <h3 className="pdp__farm-name">
                      {product.farmName ?? product.farmerDisplayName}
                    </h3>
                    {product.traceable && (
                      <span className="pdp__farm-verified">
                        <ShieldCheck /> Xác minh
                      </span>
                    )}
                  </div>
                  <div className="pdp__farm-stats">
                    {product.region && (
                      <span className="pdp__farm-stat">
                        <MapPin /> {product.region}
                      </span>
                    )}
                    <span className="pdp__farm-stat">
                      <Star style={{ fill: "#f59e0b", color: "#f59e0b" }} />
                      {product.ratingAverage ? product.ratingAverage.toFixed(1) : "5.0"}
                    </span>
                  </div>
                </div>
              </div>
              <div className="pdp__farm-actions">
                <button type="button" className="pdp__farm-btn pdp__farm-btn--outline">
                  <MessageCircle /> Nhắn tin
                </button>
                {product.farmId ? (
                  <Link to={`/marketplace/farms/${product.farmId}`} className="pdp__farm-btn pdp__farm-btn--primary">
                    <Store /> Xem nông trại
                  </Link>
                ) : (
                  <button type="button" className="pdp__farm-btn pdp__farm-btn--primary" disabled>
                    <Store /> Xem nông trại
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* ═══ TABS ═══════════════════════════════════════════ */}
        <div className="pdp__tabs-section">
          <div className="pdp__tabs-nav">
            <button
              type="button"
              className={`pdp__tab-btn${activeTab === "description" ? " pdp__tab-btn--active" : ""}`}
              onClick={() => setActiveTab("description")}
            >
              Mô tả
            </button>
            <button
              type="button"
              className={`pdp__tab-btn${activeTab === "reviews" ? " pdp__tab-btn--active" : ""}`}
              onClick={() => setActiveTab("reviews")}
            >
              Đánh giá ({product.ratingCount})
            </button>
          </div>

          <div className="pdp__tab-content">
            {/* ── Description Tab ─────────────────────────────── */}
            {activeTab === "description" && (
              <>
                {product.description && (
                  <p className="pdp__desc-text">{product.description}</p>
                )}
                <p className="pdp__desc-updated">
                  Cập nhật lần cuối: {formatDateTime(product.updatedAt)}
                </p>

                {/* Inline traceability section */}
                {product.traceable && (
                  <div className="pdp__trace-section">
                    <h3 className="pdp__trace-title">
                      <ShieldCheck /> Thông tin lô hàng
                    </h3>
                    <table className="pdp__trace-table">
                      <tbody>
                        <tr>
                          <th>Nông trại</th>
                          <td>
                            {traceabilityQuery.data?.farm?.name ?? product.farmName ?? product.farmerDisplayName}
                          </td>
                        </tr>
                        <tr>
                          <th>Khu vực</th>
                          <td>
                            {traceabilityQuery.data?.farm?.region ?? product.region ?? "Đang cập nhật"}
                          </td>
                        </tr>
                        <tr>
                          <th>Địa chỉ</th>
                          <td>
                            {traceabilityQuery.data?.farm?.address ?? "Đang cập nhật"}
                          </td>
                        </tr>
                        <tr>
                          <th>Mã lô</th>
                          <td>
                            {traceabilityQuery.data?.lot?.lotCode ?? product.traceabilityCode ?? "Đang cập nhật"}
                          </td>
                        </tr>
                        <tr>
                          <th>Mùa vụ</th>
                          <td>
                            {traceabilityQuery.data?.season?.name ?? product.seasonName ?? "Đang cập nhật"}
                          </td>
                        </tr>
                        <tr>
                          <th>Ngày thu hoạch</th>
                          <td>
                            {traceabilityQuery.data?.lot?.harvestedAt ?? "Đang cập nhật"}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}

            {/* ── Reviews Tab ─────────────────────────────────── */}
            {activeTab === "reviews" && (
              <>
                {reviewsQuery.isLoading ? (
                  <p className="pdp__reviews-loading">Đang tải đánh giá...</p>
                ) : reviewsQuery.isError ? (
                  <p className="pdp__reviews-error">Không thể tải đánh giá.</p>
                ) : reviewsQuery.data && reviewsQuery.data.items.length > 0 ? (
                  <div className="pdp__reviews-list">
                    {reviewsQuery.data.items.map((review) => (
                      <div key={review.id} className="pdp__review-item">
                        <div className="pdp__review-header">
                          <span className="pdp__review-name">{review.buyerDisplayName}</span>
                          <StarRating rating={(review as { rating?: number }).rating} />
                        </div>
                        <p className="pdp__review-date">{formatDateTime(review.createdAt)}</p>
                        <p className="pdp__review-comment">{review.comment}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="pdp__reviews-empty">Chưa có đánh giá nào cho sản phẩm này.</p>
                )}
              </>
            )}
          </div>
        </div>

        {/* ═══ RELATED PRODUCTS ═══════════════════════════════ */}
        {/* NOTE: No existing related-products hook/data — rendered only if
            farmId exists so users can navigate. Placeholder section ready for
            future integration without adding new API calls. */}
      </div>

      {/* ═══ MOBILE STICKY CTA ═══════════════════════════════ */}
      {isAuthenticated && (
        <div className="pdp__sticky-cta">
          <button
            type="button"
            className="pdp__cta-btn pdp__cta-btn--cart"
            disabled={isAdding || product.availableQuantity <= 0}
            onClick={async () => {
              await addToCart(product.id, quantity);
            }}
          >
            <ShoppingCart /> Giỏ hàng
          </button>
          <button
            type="button"
            className="pdp__cta-btn pdp__cta-btn--buy"
            disabled={isAdding || product.availableQuantity <= 0}
            onClick={async () => {
              const mode = await addToCart(product.id, quantity);
              if (mode === "server") {
                navigate("/marketplace/cart");
              }
            }}
          >
            {isAdding ? "Đang xử lý..." : "Mua ngay"}
          </button>
        </div>
      )}
    </div>
  );
}
