/**
 * SignInForm - Main sign-in form with email, password, and actions
 */

import { useI18n } from "@/shared/lib/hooks/useI18n";
import { Button, Checkbox, Input, Label } from "@/shared/ui";
import { Check, Eye, EyeOff, Loader2, LockKeyhole, Mail } from "lucide-react";
import { Link } from "react-router-dom";

interface SignInFormProps {
    email: string;
    password: string;
    keepLoggedIn: boolean;
    showPassword: boolean;
    isLoading?: boolean;
    onEmailChange: (value: string) => void;
    onPasswordChange: (value: string) => void;
    onToggleKeepLoggedIn: () => void;
    onToggleShowPassword: () => void;
    onSubmit: (e: React.FormEvent) => void;
}

export function SignInForm({
    email,
    password,
    keepLoggedIn,
    showPassword,
    isLoading = false,
    onEmailChange,
    onPasswordChange,
    onToggleKeepLoggedIn,
    onToggleShowPassword,
    onSubmit,
}: SignInFormProps) {
    const { t } = useI18n();

    const inputClass =
        "h-12 rounded-2xl border-[#dce8df] !bg-white/95 pl-11 pr-4 !text-[#173422] shadow-sm placeholder:!text-[#91a497] transition-all hover:border-[#b9dcc6] focus-visible:border-[#3BA55D] focus-visible:ring-[#3BA55D]/20 disabled:!bg-[#f4f8f5]";

    return (
        <form onSubmit={onSubmit} className="space-y-5">
            <div className="space-y-2">
                <Label htmlFor="email" className="text-sm font-bold text-[#244332]">
                    {t("auth.signIn.email")}
                    <span className="text-[#E74C3C]">*</span>
                </Label>
                <div className="relative">
                    <Mail
                        className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
                        aria-hidden="true"
                    />
                    <Input
                        type="email"
                        id="email"
                        autoComplete="email"
                        value={email}
                        onChange={(event) => onEmailChange(event.target.value)}
                        placeholder={t("auth.signIn.emailPlaceholder")}
                        className={inputClass}
                        disabled={isLoading}
                        required
                    />
                </div>
            </div>

            <div className="space-y-2">
                <Label htmlFor="password" className="text-sm font-bold text-[#244332]">
                    {t("auth.signIn.password")}
                    <span className="text-[#E74C3C]">*</span>
                </Label>
                <div className="relative">
                    <LockKeyhole
                        className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
                        aria-hidden="true"
                    />
                    <Input
                        type={showPassword ? "text" : "password"}
                        id="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(event) => onPasswordChange(event.target.value)}
                        placeholder={t("auth.signIn.passwordPlaceholder")}
                        className={`${inputClass} pr-12`}
                        disabled={isLoading}
                        required
                    />
                    <button
                        type="button"
                        onClick={onToggleShowPassword}
                        className="absolute right-3 top-1/2 inline-flex -translate-y-1/2 items-center justify-center rounded-full p-2 text-[#7b9082] transition-colors hover:bg-[#ecf9f1] hover:text-[#267241] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3BA55D]/30"
                        aria-pressed={showPassword}
                        aria-label={showPassword ? t("auth.signIn.hidePassword") : t("auth.signIn.showPassword")}
                        disabled={isLoading}
                    >
                        {showPassword ? (
                            <EyeOff className="size-4" aria-hidden="true" />
                        ) : (
                            <Eye className="size-4" aria-hidden="true" />
                        )}
                    </button>
                </div>
            </div>

            <div className="flex flex-col gap-3 text-sm sm:flex-row sm:items-center sm:justify-between">
                <label className="flex cursor-pointer items-center gap-3 text-[#335642]">
                    <Checkbox
                        id="keepLoggedIn"
                        checked={keepLoggedIn}
                        onCheckedChange={onToggleKeepLoggedIn}
                        disabled={isLoading}
                        className="size-5 rounded-md border-[#bcdcc7] data-[state=checked]:border-[#3BA55D] data-[state=checked]:bg-[#3BA55D]"
                    />
                    <span>{t("auth.signIn.keepLoggedIn")}</span>
                </label>

                <Link
                    to="/forgot-password"
                    className="font-bold text-[#2f8f4f] transition-colors hover:text-[#246f3d] hover:underline"
                >
                    {t("auth.signIn.forgotPassword")}
                </Link>
            </div>

            <Button
                variant="ghost"
                type="submit"
                disabled={isLoading}
                className="h-12 w-full rounded-2xl bg-[#3BA55D] text-sm font-bold text-white shadow-[0_14px_30px_rgba(59,165,93,0.32)] transition-all hover:-translate-y-0.5 hover:bg-[#2f8f4f] hover:shadow-[0_18px_34px_rgba(59,165,93,0.38)] focus-visible:ring-[#3BA55D]/35 disabled:translate-y-0 disabled:bg-[#9fd4b0] disabled:text-white"
            >
                {isLoading && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
                {isLoading ? t("auth.signIn.signingIn") : t("auth.signIn.signInButton")}
            </Button>

            <Link
                to="/marketplace"
                className="inline-flex h-12 w-full items-center justify-center gap-2 rounded-2xl border border-[#3BA55D]/35 bg-[#f7fcf9] px-4 text-sm font-bold text-[#2f8f4f] transition-all hover:border-[#3BA55D] hover:bg-[#ecf9f1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3BA55D]/30"
            >
                <Check className="size-4" aria-hidden="true" />
                {t("auth.signIn.continueAsGuest")}
            </Link>

            <p className="rounded-2xl bg-[#f7faf8] px-4 py-3 text-center text-xs leading-5 text-[#5f7668]">
                {t("auth.signIn.guestMarketplaceHint")}
            </p>

            <p className="pt-1 text-center text-sm text-[#45634f]">
                {t("auth.signIn.notRegistered")}{" "}
                <Link
                    to="/sign-up"
                    className="font-bold text-[#2f8f4f] transition-colors hover:text-[#246f3d] hover:underline"
                >
                    {t("auth.signIn.createAccount")}
                </Link>
            </p>
        </form>
    );
}
