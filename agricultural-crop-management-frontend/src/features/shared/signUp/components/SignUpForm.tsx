/**
 * Sign Up Form Component
 * Responsive account creation layout for the shared auth shell.
 * Implements Progressive Profiling (Multi-step)
 */

import { useI18n } from "@/shared/lib/hooks/useI18n";
import { cn } from "@/shared/lib";
import { Button, Checkbox, Input, Label } from "@/shared/ui";
import { Check, Eye, EyeOff, Loader2, LockKeyhole, Mail, Phone, UserRound, ArrowLeft } from "lucide-react";
import { useState, type BaseSyntheticEvent, type ReactNode } from "react";
import { Controller, type UseFormReturn } from "react-hook-form";
import { Link } from "react-router-dom";
import type { SignUpFormData } from "../types";
import { RoleSelector } from "./RoleSelector";

interface SignUpFormProps {
  form: UseFormReturn<SignUpFormData>;
  showPassword: boolean;
  showConfirmPassword: boolean;
  onToggleShowPassword: () => void;
  onToggleShowConfirmPassword: () => void;
  onSubmit: (event?: BaseSyntheticEvent) => void;
}

export function SignUpForm({
  form,
  showPassword,
  showConfirmPassword,
  onToggleShowPassword,
  onToggleShowConfirmPassword,
  onSubmit,
}: SignUpFormProps) {
  const { t } = useI18n();
  const [step, setStep] = useState<1 | 2>(1);
  const {
    register,
    control,
    watch,
    trigger,
    formState: { errors, isSubmitting },
  } = form;

  const passwordValue = watch("password") ?? "";
  const passwordRules = [
    { key: "minLength", met: passwordValue.length >= 8 },
    { key: "uppercase", met: /[A-Z]/.test(passwordValue) },
    { key: "lowercase", met: /[a-z]/.test(passwordValue) },
    { key: "number", met: /\d/.test(passwordValue) },
    { key: "special", met: /[@$!%*?&]/.test(passwordValue) },
  ];

  const inputClass = (hasError: boolean) =>
    cn(
      "h-12 rounded-2xl border-[#dce8df] !bg-white/95 pl-11 pr-4 !text-[#173422] shadow-sm placeholder:!text-[#91a497] transition-all hover:border-[#b9dcc6] focus-visible:border-[#3BA55D] focus-visible:ring-[#3BA55D]/20 disabled:!bg-[#f4f8f5]",
      hasError && "border-[#E74C3C] focus-visible:border-[#E74C3C] focus-visible:ring-[#E74C3C]/20",
    );

  const handleNextStep = async () => {
    const isStep1Valid = await trigger(["email", "password", "confirmPassword", "termsAccepted"]);
    if (isStep1Valid) {
      setStep(2);
    }
  };

  return (
    <form onSubmit={onSubmit} className="max-w-full space-y-5">
      {step === 1 && (
        <div className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
          <FieldShell
            id="email"
            label={t("auth.signUp.email")}
            required
            error={errors.email?.message}
          >
            <Mail
              className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
              aria-hidden="true"
            />
            <Input
              type="email"
              id="email"
              autoComplete="email"
              placeholder={t("auth.signUp.emailPlaceholder")}
              className={inputClass(!!errors.email)}
              aria-invalid={!!errors.email}
              disabled={isSubmitting}
              {...register("email")}
            />
          </FieldShell>

          <FieldShell
            id="password"
            label={t("auth.signUp.password")}
            required
            error={errors.password?.message}
          >
            <LockKeyhole
              className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
              aria-hidden="true"
            />
            <Input
              type={showPassword ? "text" : "password"}
              id="password"
              autoComplete="new-password"
              placeholder={t("auth.signUp.passwordPlaceholder")}
              className={`${inputClass(!!errors.password)} pr-12`}
              aria-invalid={!!errors.password}
              disabled={isSubmitting}
              {...register("password")}
            />
            <PasswordToggle
              showPassword={showPassword}
              onToggle={onToggleShowPassword}
              disabled={isSubmitting}
              label={showPassword ? t("auth.signIn.hidePassword") : t("auth.signIn.showPassword")}
            />
          </FieldShell>

          <FieldShell
            id="confirmPassword"
            label={t("auth.signUp.confirmPassword")}
            required
            error={errors.confirmPassword?.message}
          >
            <LockKeyhole
              className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
              aria-hidden="true"
            />
            <Input
              type={showConfirmPassword ? "text" : "password"}
              id="confirmPassword"
              autoComplete="new-password"
              placeholder={t("auth.signUp.confirmPasswordPlaceholder")}
              className={`${inputClass(!!errors.confirmPassword)} pr-12`}
              aria-invalid={!!errors.confirmPassword}
              disabled={isSubmitting}
              {...register("confirmPassword")}
            />
            <PasswordToggle
              showPassword={showConfirmPassword}
              onToggle={onToggleShowConfirmPassword}
              disabled={isSubmitting}
              label={showConfirmPassword ? t("auth.signIn.hidePassword") : t("auth.signIn.showPassword")}
            />
          </FieldShell>

          <div className="rounded-2xl border border-[#dce8df] bg-[#f8fcf9] p-4">
            <p className="text-sm font-bold text-[#244332]">{t("auth.signUp.passwordRules.title")}</p>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              {passwordRules.map((rule) => (
                <div
                  key={rule.key}
                  className={cn(
                    "flex items-center gap-2 text-xs font-medium",
                    rule.met ? "text-[#2f8f4f]" : "text-[#789083]",
                  )}
                >
                  <span
                    className={cn(
                      "flex size-4 items-center justify-center rounded-full border",
                      rule.met ? "border-[#3BA55D] bg-[#3BA55D] text-white" : "border-[#c9dbcf] bg-white",
                    )}
                  >
                    {rule.met && <Check className="size-3" aria-hidden="true" />}
                  </span>
                  {t(`auth.signUp.passwordRules.${rule.key}`)}
                </div>
              ))}
            </div>
          </div>

          <div>
            <Controller
              control={control}
              name="termsAccepted"
              render={({ field: { onChange, value } }) => (
                <div className="flex items-start gap-3 rounded-2xl bg-[#f7faf8] px-4 py-3">
                  <Checkbox
                    checked={value === true}
                    onCheckedChange={(checked) => onChange(checked === true)}
                    disabled={isSubmitting}
                    aria-invalid={!!errors.termsAccepted}
                    className="mt-0.5 size-5 rounded-md border-[#bcdcc7] data-[state=checked]:border-[#3BA55D] data-[state=checked]:bg-[#3BA55D]"
                  />
                  <span
                    className="text-sm leading-6 text-[#45634f] cursor-pointer"
                    onClick={() => onChange(!value)}
                  >
                    {t("auth.signUp.termsPrefix")}{" "}
                    <Link
                      to="/terms"
                      className="font-bold text-[#2f8f4f] hover:underline"
                      onClick={(event) => event.stopPropagation()}
                    >
                      {t("auth.signUp.termsLink")}
                    </Link>{" "}
                    {t("common.and")}{" "}
                    <Link
                      to="/privacy"
                      className="font-bold text-[#2f8f4f] hover:underline"
                      onClick={(event) => event.stopPropagation()}
                    >
                      {t("auth.signUp.privacyLink")}
                    </Link>
                    <span className="ml-0.5 text-[#E74C3C]">*</span>
                  </span>
                </div>
              )}
            />
            {errors.termsAccepted && (
              <p className="mt-2 text-xs font-medium text-[#E74C3C]">
                {errors.termsAccepted.message}
              </p>
            )}
          </div>

          <Button
            variant="ghost"
            type="button"
            onClick={handleNextStep}
            className="h-12 w-full rounded-2xl bg-[#3BA55D] text-sm font-bold text-white shadow-[0_14px_30px_rgba(59,165,93,0.32)] transition-all hover:-translate-y-0.5 hover:bg-[#2f8f4f] hover:shadow-[0_18px_34px_rgba(59,165,93,0.38)] focus-visible:ring-[#3BA55D]/35"
          >
            {t("common.continue", "Continue")}
          </Button>
        </div>
      )}

      {step === 2 && (
        <div className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-300">
          <div className="flex items-center gap-2 mb-2">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="inline-flex size-8 items-center justify-center rounded-full text-[#5f7668] transition-colors hover:bg-[#ecf9f1] hover:text-[#267241] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3BA55D]/30"
              aria-label="Back to previous step"
            >
              <ArrowLeft className="size-4" aria-hidden="true" />
            </button>
            <p className="text-sm font-medium text-[#45634f]">
              {t("auth.signUp.step2Hint", "Almost done! Tell us a bit about yourself.")}
            </p>
          </div>

          <FieldShell
            id="fullName"
            label={t("auth.signUp.fullName")}
            required
            error={errors.fullName?.message}
          >
            <UserRound
              className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
              aria-hidden="true"
            />
            <Input
              type="text"
              id="fullName"
              autoComplete="name"
              placeholder={t("auth.signUp.fullNamePlaceholder")}
              className={inputClass(!!errors.fullName)}
              aria-invalid={!!errors.fullName}
              disabled={isSubmitting}
              {...register("fullName")}
            />
          </FieldShell>

          <FieldShell
            id="phoneNumber"
            label={t("auth.signUp.phoneNumber")}
            optionalText={t("common.optional")}
            error={errors.phoneNumber?.message}
          >
            <Phone
              className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#3BA55D]"
              aria-hidden="true"
            />
            <Input
              type="tel"
              id="phoneNumber"
              autoComplete="tel"
              placeholder={t("auth.signUp.phoneNumberPlaceholder")}
              className={inputClass(!!errors.phoneNumber)}
              aria-invalid={!!errors.phoneNumber}
              disabled={isSubmitting}
              {...register("phoneNumber")}
            />
          </FieldShell>

          <Controller
            control={control}
            name="role"
            render={({ field }) => (
              <RoleSelector
                name={field.name}
                selectedRole={field.value ?? "FARMER"}
                onRoleChange={field.onChange}
                onBlur={field.onBlur}
                errorMessage={errors.role?.message}
              />
            )}
          />

          <Button
            variant="ghost"
            type="submit"
            disabled={isSubmitting}
            className="h-12 w-full rounded-2xl bg-[#3BA55D] text-sm font-bold text-white shadow-[0_14px_30px_rgba(59,165,93,0.32)] transition-all hover:-translate-y-0.5 hover:bg-[#2f8f4f] hover:shadow-[0_18px_34px_rgba(59,165,93,0.38)] focus-visible:ring-[#3BA55D]/35 disabled:translate-y-0 disabled:bg-[#9fd4b0] disabled:text-white"
          >
            {isSubmitting && <Loader2 className="size-4 animate-spin" aria-hidden="true" />}
            {isSubmitting ? t("auth.signUp.creatingAccount") : t("auth.signUp.createAccount")}
          </Button>
        </div>
      )}

      <p className="pt-2 text-center text-sm text-[#45634f]">
        {t("auth.signUp.hasAccount")}{" "}
        <Link
          to="/sign-in"
          className="font-bold text-[#2f8f4f] transition-colors hover:text-[#246f3d] hover:underline"
        >
          {t("auth.signUp.signIn")}
        </Link>
      </p>
    </form>
  );
}

function FieldShell({
  id,
  label,
  required,
  optionalText,
  error,
  children,
}: {
  id: string;
  label: string;
  required?: boolean;
  optionalText?: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id} className="text-sm font-bold text-[#244332]">
        {label}
        {required && <span className="text-[#E74C3C]">*</span>}
        {optionalText && <span className="ml-1 font-medium text-[#789083]">({optionalText})</span>}
      </Label>
      <div className="relative">{children}</div>
      {error && <p className="text-xs font-medium text-[#E74C3C]">{error}</p>}
    </div>
  );
}

function PasswordToggle({
  showPassword,
  onToggle,
  disabled,
  label,
}: {
  showPassword: boolean;
  onToggle: () => void;
  disabled: boolean;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className="absolute right-3 top-1/2 rounded-full p-2 text-[#7b9082] transition-colors hover:bg-[#ecf9f1] hover:text-[#267241] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3BA55D]/30"
      aria-label={label}
      aria-pressed={showPassword}
      disabled={disabled}
    >
      {showPassword ? (
        <EyeOff className="size-4" aria-hidden="true" />
      ) : (
        <Eye className="size-4" aria-hidden="true" />
      )}
    </button>
  );
}
