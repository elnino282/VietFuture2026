import { PackageCheck, ShieldCheck } from "lucide-react";
import type { ChatWidgetContext } from "../model/widgetTypes";
import {
  getTransactionStatusClass,
  getTransactionStatusLabel,
} from "../lib/chatDisplayHelpers";

type ChatWidgetContextCardProps = {
  context: ChatWidgetContext;
};

export function ChatWidgetContextCard({ context }: ChatWidgetContextCardProps) {
  return (
    <section className="chat-widget-context" aria-label="Order context">
      <div className="chat-widget-context__image" aria-hidden="true">
        {context.imageUrl ? (
          <img src={context.imageUrl} alt="" referrerPolicy="no-referrer" />
        ) : (
          <PackageCheck className="h-5 w-5" />
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="chat-widget-context__row">
          <h4>{context.title}</h4>
          <span className={getTransactionStatusClass(context.transactionStatus)}>
            {getTransactionStatusLabel(context.transactionStatus)}
          </span>
        </div>
        <p>{context.subtitle}</p>
        {context.price ? (
          <p className="chat-widget-context__price">
            {context.price}
            {context.quantity ? (
              <span className="chat-widget-context__qty"> · {context.quantity}</span>
            ) : null}
          </p>
        ) : null}
        <small>
          <ShieldCheck className="chat-widget-context__trace-icon" aria-hidden="true" />
          Mã truy xuất: {context.traceCode}
        </small>
      </div>
    </section>
  );
}
