import { useEffect, useState } from "react";
import { Leaf, ShieldCheck, Truck } from "lucide-react";
import { cn } from "@/shared/lib";
import { ImagePlaceholder } from "@/shared/ui";
import { FeatureItem } from "./types";
import { FEATURE_IMAGES } from "./featureImages";

const features: FeatureItem[] = [
  {
    id: "traceability",
    icon: <ShieldCheck size={28} aria-hidden="true" />,
    title: "Truy xuất rõ ràng",
    description: "Xem nông trại, mùa vụ và lô thu hoạch ngay trên từng sản phẩm. Khởi nguồn từ gốc rễ, mang lại sự tin cậy tuyệt đối.",
    imageUrl: FEATURE_IMAGES.traceability,
  },
  {
    id: "transparency",
    icon: <Leaf size={28} aria-hidden="true" />,
    title: "Sản phẩm minh bạch",
    description: "Người bán chỉ đăng được sản phẩm gắn với tồn kho thu hoạch hiện có. Không bán khống, không gian lận.",
    imageUrl: FEATURE_IMAGES.transparency,
  },
  {
    id: "direct-purchase",
    icon: <Truck size={28} aria-hidden="true" />,
    title: "Đặt mua trực tiếp",
    description: "Giữ trải nghiệm thương mại điện tử quen thuộc với giỏ hàng, checkout và đơn hàng thật đến tận tay bạn.",
    imageUrl: FEATURE_IMAGES.directPurchase,
  },
];

function FeatureImage({ src, alt }: { src?: string; alt: string }) {
  const [hasError, setHasError] = useState(false);

  if (hasError || !src) {
    return <ImagePlaceholder />;
  }

  return (
    <img 
      src={src} 
      alt={alt} 
      className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"
      loading="lazy"
      onError={() => setHasError(true)}
    />
  );
}

export function MarketHomeFeatures() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <section className="bg-surface-inverse py-16 lg:py-24 relative z-10 -mt-8 rounded-t-[3rem] rounded-b-[3rem] sm:-mt-12 sm:rounded-t-[4rem] sm:rounded-b-[4rem] overflow-hidden">
      <div className="mx-auto max-w-enterprise px-6 relative z-10">
        <div className="grid grid-cols-1 gap-16 lg:grid-cols-12 lg:gap-12">
          
          {/* Asymmetrical Left Side: Sticky Narrative */}
          <div className="lg:col-span-5 lg:sticky lg:top-32 lg:h-fit">
            <h2 className="font-heading text-4xl font-bold tracking-tight text-earth-900 sm:text-5xl lg:text-6xl leading-tight">
              Định hình lại <br/>cách bạn mua <br/><span className="text-primary italic">nông sản.</span>
            </h2>
            <p className="mt-8 text-xl leading-relaxed text-earth-700">
              Không còn những thông tin mập mờ về nguồn gốc. Chúng tôi đưa bạn đến tận vườn cây, 
              hiểu rõ từng lô thu hoạch trước khi quyết định đặt hàng. 
            </p>
            <p className="mt-6 text-lg leading-relaxed text-earth-600 font-medium">
              Một tiêu chuẩn mới cho nền nông nghiệp minh bạch, nơi bạn biết chính xác mình đang tiêu thụ điều gì.
            </p>
            
            <div className="mt-12 h-[1px] w-12 bg-accent/50"></div>
          </div>

          {/* Asymmetrical Right Side: Scrolling Image Cards */}
          <div className="lg:col-span-7">
            <div className="flex flex-col gap-24 sm:gap-32">
              {features.map((feature, index) => (
                <div
                  key={feature.id}
                  className={cn(
                    "flex flex-col transition-all duration-1000 ease-out",
                    mounted ? "translate-y-0 opacity-100" : "translate-y-24 opacity-0"
                  )}
                  style={{ transitionDelay: `${index * 200}ms` }}
                >
                  <div className="relative aspect-[4/3] w-full overflow-hidden rounded-3xl mb-8 group">
                    <FeatureImage src={feature.imageUrl} alt={feature.title} />
                    {/* Subtle gradient overlay to ensure the image sits well in dark mode */}
                    <div className="absolute inset-0 bg-earth-950/20 mix-blend-multiply group-hover:bg-transparent transition-colors duration-700"></div>
                  </div>
                  
                  <div className="flex flex-col sm:flex-row gap-6 items-start">
                    <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary border border-primary/20 backdrop-blur-sm">
                      {feature.icon}
                    </div>
                    <div className="pt-2">
                      <h3 className="mb-4 text-2xl font-bold text-earth-900 font-heading tracking-tight">{feature.title}</h3>
                      <p className="text-lg text-earth-700 leading-relaxed max-w-xl">
                        {feature.description}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
        </div>
      </div>
    </section>
  );
}
